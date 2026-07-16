import * as dotenv from 'dotenv';
dotenv.config();

export const settings = Object.freeze({
  db: {
    host: process.env.DB_HOST || 'localhost',
    port: parseInt(process.env.DB_PORT || '5432', 10),
    user: process.env.DB_USER || 'postgres',
    password: process.env.DB_PASSWORD || 'password',
    database: process.env.DB_NAME || 'shortbrew',
    poolSize: parseInt(process.env.DB_POOL_SIZE || '5', 10),
  },
  rabbitmq: {
    url: `amqp://${process.env.RABBITMQ_USER || 'guest'}:${process.env.RABBITMQ_PASSWORD || 'guest'}@${process.env.RABBITMQ_HOST || 'localhost'}:${process.env.RABBITMQ_PORT || '5672'}`,
    exchangeName: 'click_events',
    queueName: 'click_events.process',
    routingKey: 'click.created',
    prefetchCount: parseInt(process.env.WORKER_PREFETCH_COUNT || '10', 10),
    maxRetries: parseInt(process.env.CLICK_EVENT_MAX_RETRIES || '3', 10),
  }
});
