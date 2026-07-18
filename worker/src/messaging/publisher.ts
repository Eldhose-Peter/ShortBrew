import amqplib from 'amqplib';
import { ClickEvent } from '../types';
import { settings } from '../config/settings';
import { getLogContext } from '../utils/context';
import { logger } from '../utils/logger';

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

  const context = getLogContext();
  if (!payload.request_id && context.request_id) payload.request_id = context.request_id;
  if (!payload.trace_id && context.trace_id) payload.trace_id = context.trace_id;
  if (!payload.span_id && context.span_id) payload.span_id = context.span_id;

  const backoffSeconds = Math.min(Math.pow(2, retryCount), 30);

  logger.warn('click_event_retry_scheduled', {
    retry_count: retryCount,
    backoff_seconds: backoffSeconds,
    url_id: payload.url_id
  });

  await sleep(backoffSeconds * 1000);

  const headers: Record<string, string> = {};
  if (payload.request_id) headers['x-request-id'] = payload.request_id;
  if (payload.trace_id) headers['x-trace-id'] = payload.trace_id;
  if (payload.span_id) headers['x-span-id'] = payload.span_id;

  const messageBuffer = Buffer.from(JSON.stringify(payload));
  channel.publish(
    settings.rabbitmq.exchangeName,
    settings.rabbitmq.routingKey,
    messageBuffer,
    {
      deliveryMode: 2, // Persistent
      contentType: 'application/json',
      headers
    }
  );
}
