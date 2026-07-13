import amqplib from 'amqplib';
import { ClickEvent, processClickEvent } from './db';

const RABBITMQ_URL = process.env.RABBITMQ_URL || 'amqp://guest:guest@localhost:5672';
const EXCHANGE_NAME = 'click_events';
const QUEUE_NAME = 'click_events.process';
const ROUTING_KEY = 'click.created';

export async function startWorker(): Promise<void> {
  try {
    console.log('Connecting to RabbitMQ...');
    const connection = await amqplib.connect(RABBITMQ_URL);
    const channel = await connection.createChannel();

    // Assert Exchange and Queue
    await channel.assertExchange(EXCHANGE_NAME, 'topic', { durable: true });
    await channel.assertQueue(QUEUE_NAME, { durable: true });
    await channel.bindQueue(QUEUE_NAME, EXCHANGE_NAME, ROUTING_KEY);

    // Limit unacknowledged messages
    await channel.prefetch(10);

    console.log(`[*] Worker active. Listening for messages on queue '${QUEUE_NAME}' bound with key '${ROUTING_KEY}'`);

    await channel.consume(QUEUE_NAME, async (msg) => {
      if (!msg) return;

      try {
        const payload: ClickEvent = JSON.parse(msg.content.toString());
        await processClickEvent(payload);
        channel.ack(msg);
      } catch (err) {
        console.error('[!] Error processing message. Requeuing message.', err);
        // Nack and requeue
        channel.nack(msg, false, true);
      }
    }, { noAck: false });

  } catch (error) {
    console.error('[!] Worker failed to start:', error);
    process.exit(1);
  }
}
