import { Pool } from 'pg';

export const pool = new Pool({
  host: process.env.DB_HOST || 'localhost',
  port: parseInt(process.env.DB_PORT || '5432'),
  user: process.env.DB_USER || 'postgres',
  password: process.env.DB_PASSWORD || 'password',
  database: process.env.DB_NAME || 'shortbrew',
});

export interface ClickEvent {
  url_id: number;
  short_code: string;
  referrer?: string;
  user_agent?: string;
  ip_hash?: string;
  clicked_at: string;
  retry_count: number;
}

function getDeviceType(userAgent?: string): string {
  if (!userAgent) return 'Unknown';
  const ua = userAgent.toLowerCase();
  if (ua.includes('mobile') || ua.includes('android') || ua.includes('iphone') || ua.includes('ipad')) {
    return 'Mobile';
  }
  if (ua.includes('tablet')) {
    return 'Tablet';
  }
  return 'Desktop';
}

function getReferrerDomain(referrer?: string): string {
  if (!referrer || referrer.trim() === '') return 'Direct';
  try {
    const url = new URL(referrer);
    return url.hostname;
  } catch {
    return 'Unknown';
  }
}

export async function processClickEvent(payload: ClickEvent): Promise<void> {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    // 1. Determine stat details
    const clickedDate = new Date(payload.clicked_at);
    const statDate = clickedDate.toISOString().split('T')[0];
    const deviceType = getDeviceType(payload.user_agent);
    const referrerDomain = getReferrerDomain(payload.referrer);

    // 2. Insert the raw event into click_events
    const insertEventQuery = `
      INSERT INTO click_events (url_id, clicked_at, referrer, device_type, ip_hash, country_code)
      VALUES ($1, $2, $3, $4, $5, $6)
    `;
    await client.query(insertEventQuery, [
      payload.url_id,
      clickedDate,
      payload.referrer ? payload.referrer.substring(0, 512) : null,
      deviceType,
      payload.ip_hash || null,
      null // country_code
    ]);

    // 3. Increment total_clicks on the urls table
    const updateUrlQuery = `
      UPDATE urls
      SET total_clicks = total_clicks + 1
      WHERE id = $1
    `;
    await client.query(updateUrlQuery, [payload.url_id]);

    // 4. Upsert the stats in url_daily_stats
    const statsInitQuery = `
      INSERT INTO url_daily_stats (url_id, stat_date, click_count, referrer_breakdown)
      VALUES ($1, $2, 0, '{}'::jsonb)
      ON CONFLICT (url_id, stat_date) DO NOTHING
    `;
    await client.query(statsInitQuery, [payload.url_id, statDate]);

    const statsUpdateQuery = `
      UPDATE url_daily_stats
      SET click_count = click_count + 1,
          referrer_breakdown = jsonb_set(
            coalesce(referrer_breakdown, '{}'::jsonb),
            ARRAY[$3],
            to_jsonb(coalesce((referrer_breakdown->>$3)::int, 0) + 1),
            true
          )
      WHERE url_id = $1 AND stat_date = $2
    `;
    await client.query(statsUpdateQuery, [payload.url_id, statDate, referrerDomain]);

    await client.query('COMMIT');
    console.log(`[x] Successfully processed click event: url_id=${payload.url_id}, code=${payload.short_code}`);
  } catch (error) {
    await client.query('ROLLBACK');
    console.error(`[!] Failed to process click event for url_id=${payload.url_id}:`, error);
    throw error;
  } finally {
    client.release();
  }
}
