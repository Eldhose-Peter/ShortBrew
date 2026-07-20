import * as dotenv from 'dotenv';
dotenv.config();

export const settings = Object.freeze({
  db: {
    host: process.env.POSTGRES_HOST || 'localhost',
    port: parseInt(process.env.POSTGRES_PORT || '5432', 10),
    user: process.env.POSTGRES_USER || 'postgres',
    password: process.env.POSTGRES_PASSWORD || 'password',
    database: process.env.POSTGRES_DB || 'shortbrew',
    poolSize: parseInt(process.env.POSTGRES_POOL_SIZE || '5', 10),
  },
  rabbitmq: {
    url: `amqp://${process.env.RABBITMQ_USER || 'guest'}:${process.env.RABBITMQ_PASSWORD || 'guest'}@${process.env.RABBITMQ_HOST || 'localhost'}:${process.env.RABBITMQ_PORT || '5672'}`,
    exchangeName: 'click_events',
    queueName: 'click_events.process',
    routingKey: 'click.created',
    prefetchCount: parseInt(process.env.WORKER_PREFETCH_COUNT || '10', 10),
    maxRetries: parseInt(process.env.CLICK_EVENT_MAX_RETRIES || '3', 10),
  },
  redis: {
    host: process.env.REDIS_HOST || 'localhost',
    port: parseInt(process.env.REDIS_PORT || '6379', 10),
  }
});
