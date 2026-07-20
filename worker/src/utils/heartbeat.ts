import Redis from 'ioredis';
import { settings } from '../config/settings';
import { logger } from './logger';

let heartbeatTimer: NodeJS.Timeout | null = null;
let redisClient: Redis | null = null;

/**
 * Starts sending periodic heartbeat pings (every 5 seconds) to the Redis hash 'worker:heartbeats'.
 */
export function startHeartbeat(): void {
  const workerId = process.env.WORKER_ID || `worker-${process.pid}`;

  try {
    redisClient = new Redis({
      host: settings.redis.host,
      port: settings.redis.port,
      lazyConnect: true,
      maxRetriesPerRequest: 1,
    });

    redisClient.connect().then(() => {
      logger.info('Connected to Redis for worker heartbeats', { workerId });

      heartbeatTimer = setInterval(async () => {
        try {
          if (redisClient) {
            await redisClient.hset('worker:heartbeats', workerId, Date.now().toString());
          }
        } catch (err) {
          logger.error('Failed to send heartbeat to Redis', err);
        }
      }, 5000);
    }).catch((err) => {
      logger.error('Could not connect to Redis for worker heartbeat (failing open)', err);
    });
  } catch (err) {
    logger.error('Failed to initialize heartbeat module', err);
  }
}

/**
 * Gracefully stops the heartbeat timer and closes the Redis connection on worker shutdown.
 */
export async function stopHeartbeat(): Promise<void> {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer);
    heartbeatTimer = null;
  }
  if (redisClient) {
    try {
      await redisClient.quit();
    } catch {
      // Ignore disconnect errors during shutdown
    }
  }
}
