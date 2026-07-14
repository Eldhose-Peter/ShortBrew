import { dbPool } from './connection';
import { ClickEvent } from '../types';
import { mockCountryForIpHash } from '../utils/geo-mock';
import { classifyDevice } from '../utils/device-classifier';

/**
 * Persists the click raw event and increments click aggregates in the database.
 */
export async function persistClick(payload: ClickEvent): Promise<void> {
  const client = await dbPool.connect();
  try {
    await client.query('BEGIN');

    const clickedDate = new Date(payload.clicked_at);
    const statDate = clickedDate.toISOString().split('T')[0];
    const country = mockCountryForIpHash(payload.ip_hash);
    const device = classifyDevice(payload.user_agent);

    // 1. Log raw event
    const insertEventQuery = `
      INSERT INTO click_events (url_id, clicked_at, referrer, device_type, ip_hash, country_code)
      VALUES ($1, $2, $3, $4, $5, $6)
    `;
    await client.query(insertEventQuery, [
      payload.url_id,
      clickedDate,
      payload.referrer ? payload.referrer.substring(0, 512) : null,
      device,
      payload.ip_hash || null,
      country
    ]);

    // 2. Increment click count
    const updateUrlQuery = `
      UPDATE urls
      SET total_clicks = total_clicks + 1
      WHERE id = $1
    `;
    await client.query(updateUrlQuery, [payload.url_id]);

    // 3. Upsert daily stat breakdowns
    const referrerKey = payload.referrer || 'direct';
    const countryKey = country || 'XX';

    const upsertStatsQuery = `
      INSERT INTO url_daily_stats (url_id, stat_date, click_count, referrer_breakdown, country_breakdown)
      VALUES ($1, $2, 1, $3::jsonb, $4::jsonb)
      ON CONFLICT (url_id, stat_date) DO UPDATE SET
        click_count = url_daily_stats.click_count + 1,
        referrer_breakdown = jsonb_set(
          coalesce(url_daily_stats.referrer_breakdown, '{}'::jsonb),
          ARRAY[$5],
          to_jsonb(coalesce((url_daily_stats.referrer_breakdown->>$5)::int, 0) + 1),
          true
        ),
        country_breakdown = jsonb_set(
          coalesce(url_daily_stats.country_breakdown, '{}'::jsonb),
          ARRAY[$6],
          to_jsonb(coalesce((url_daily_stats.country_breakdown->>$6)::int, 0) + 1),
          true
        )
    `;
    await client.query(upsertStatsQuery, [
      payload.url_id,
      statDate,
      JSON.stringify({ [referrerKey]: 1 }),
      JSON.stringify({ [countryKey]: 1 }),
      referrerKey,
      countryKey
    ]);

    await client.query('COMMIT');
  } catch (error) {
    await client.query('ROLLBACK');
    throw error;
  } finally {
    client.release();
  }
}
