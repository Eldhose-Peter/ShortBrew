import amqplib from 'amqplib';
import { ClickEvent } from '../types';
import { persistClick } from '../database/repository';
import { republishWithBackoff } from './publisher';
import { settings } from '../config/settings';

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
    console.error(JSON.stringify({ event: 'click_event_malformed_payload_dropping' }));
    channel.ack(msg);
    return;
  }

  try {
    await persistClick(payload);
    channel.ack(msg);
    console.log(JSON.stringify({ event: 'click_event_processed', url_id: payload.url_id }));
  } catch (error) {
    console.error(JSON.stringify({
      event: 'click_event_processing_failed',
      url_id: payload.url_id,
      error: error instanceof Error ? error.stack : String(error)
    }));

    const retryCount = payload.retry_count || 0;

    if (retryCount < settings.rabbitmq.maxRetries) {
      await republishWithBackoff(channel, payload);
      channel.ack(msg); // original superseded by the republished retry copy
    } else {
      console.error(JSON.stringify({
        event: 'click_event_max_retries_exceeded_dead_lettering',
        url_id: payload.url_id
      }));
      channel.nack(msg, false, false); // route to DLQ
    }
  }
}

/**
 * Connects to RabbitMQ, asserts topology (exchange, queue, binding) and listens for click event messages.
 */
export async function startWorker(): Promise<void> {
  try {
    console.log('Connecting to RabbitMQ...');
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

    console.log(`[*] Worker active. Listening for messages on queue '${settings.rabbitmq.queueName}' bound with key '${settings.rabbitmq.routingKey}' (prefetch: ${settings.rabbitmq.prefetchCount})`);

    await channel.consume(settings.rabbitmq.queueName, async (msg) => {
      if (msg) {
        await handleMessage(channel, msg);
      }
    }, { noAck: false });

  } catch (error) {
    console.error('[!] Worker failed to start:', error);
    process.exit(1);
  }
}
