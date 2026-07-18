import amqplib from 'amqplib';
import { ClickEvent } from '../types';
import { persistClick } from '../database/repository';
import { republishWithBackoff } from './publisher';
import { settings } from '../config/settings';
import { runWithContext } from '../utils/context';
import { logger } from '../utils/logger';

/**
 * Handles incoming RabbitMQ click event messages, applying parsing validations, database persistence,
 * and retry/DLQ backoff routing.
 */
async function handleMessage(
  channel: amqplib.Channel,
  msg: amqplib.ConsumeMessage
): Promise<void> {
  let payload: ClickEvent;
  try {
    payload = JSON.parse(msg.content.toString());
  } catch (err) {
    logger.error('click_event_malformed_payload_dropping', err);
    channel.ack(msg);
    return;
  }

  const headers = msg.properties.headers || {};
  const requestId = payload.request_id || (headers['x-request-id'] as string) || (headers['request_id'] as string);
  const traceId = payload.trace_id || (headers['x-trace-id'] as string) || (headers['trace_id'] as string);
  const spanId = payload.span_id || (headers['x-span-id'] as string) || (headers['span_id'] as string);

  await runWithContext({ request_id: requestId, trace_id: traceId, span_id: spanId }, async () => {
    try {
      await persistClick(payload);
      channel.ack(msg);
      logger.info('click_event_processed', { url_id: payload.url_id, short_code: payload.short_code });
    } catch (error) {
      logger.error('click_event_processing_failed', error, { url_id: payload.url_id });

      const retryCount = payload.retry_count || 0;

      if (retryCount < settings.rabbitmq.maxRetries) {
        await republishWithBackoff(channel, payload);
        channel.ack(msg); // original superseded by the republished retry copy
      } else {
        logger.error('click_event_max_retries_exceeded_dead_lettering', undefined, { url_id: payload.url_id });
        channel.nack(msg, false, false); // route to DLQ
      }
    }
  });
}

/**
 * Connects to RabbitMQ, asserts topology (exchange, queue, binding) and listens for click event messages.
 */
export async function startWorker(): Promise<void> {
  try {
    logger.info('Connecting to RabbitMQ...');
    const connection = await amqplib.connect(settings.rabbitmq.url);
    const channel = await connection.createChannel();

    // Assert Exchange and Queue
    await channel.assertExchange(settings.rabbitmq.exchangeName, 'topic', { durable: true });
    await channel.assertQueue(settings.rabbitmq.queueName, { durable: true });
    await channel.bindQueue(
      settings.rabbitmq.queueName,
      settings.rabbitmq.exchangeName,
      settings.rabbitmq.routingKey
    );

    await channel.prefetch(settings.rabbitmq.prefetchCount);

    logger.info('Worker active. Listening for messages', {
      queue: settings.rabbitmq.queueName,
      routingKey: settings.rabbitmq.routingKey,
      prefetch: settings.rabbitmq.prefetchCount
    });

    await channel.consume(settings.rabbitmq.queueName, async (msg) => {
      if (msg) {
        await handleMessage(channel, msg);
      }
    }, { noAck: false });

  } catch (error) {
    logger.error('Worker failed to start', error);
    process.exit(1);
  }
}
