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

    await client.query('COMMIT');
  } catch (error) {
    await client.query('ROLLBACK');
    throw error;
  } finally {
    client.release();
  }
}
