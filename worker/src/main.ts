import { startWorker } from './messaging/consumer';
import { dbPool } from './database/connection';
import { logger } from './utils/logger';
import { startHeartbeat, stopHeartbeat } from './utils/heartbeat';

const shutdown = async () => {
  logger.info('Shutting down. Closing heartbeat and database pool...');
  try {
    await stopHeartbeat();
    await dbPool.end();
    logger.info('Database pool closed successfully.');
    process.exit(0);
  } catch (err) {
    logger.error('Error closing database pool:', err);
    process.exit(1);
  }
};

process.on('SIGTERM', shutdown);
process.on('SIGINT', shutdown);

// Start worker heartbeat and RabbitMQ message consumer
startHeartbeat();
startWorker();
