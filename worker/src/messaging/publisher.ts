import amqplib from 'amqplib';
import { ClickEvent } from '../types';
import { settings } from '../config/settings';

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * Reschedules a failed message publishing copy back to exchange after a backoff delay.
 */
export async function republishWithBackoff(
  channel: amqplib.Channel,
  payload: ClickEvent
): Promise<void> {
  const retryCount = (payload.retry_count || 0) + 1;
  payload.retry_count = retryCount;
  const backoffSeconds = Math.min(Math.pow(2, retryCount), 30);

  console.warn(JSON.stringify({
    event: 'click_event_retry_scheduled',
    retry_count: retryCount,
    backoff_seconds: backoffSeconds,
    url_id: payload.url_id
  }));

  await sleep(backoffSeconds * 1000);

  const messageBuffer = Buffer.from(JSON.stringify(payload));
  channel.publish(
    settings.rabbitmq.exchangeName,
    settings.rabbitmq.routingKey,
    messageBuffer,
    {
      deliveryMode: 2, // Persistent
      contentType: 'application/json'
    }
  );
}
