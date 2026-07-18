import { startWorker } from './messaging/consumer';
import { dbPool } from './database/connection';
import { logger } from './utils/logger';

const shutdown = async () => {
  logger.info('Shutting down. Closing database pool...');
  try {
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

// Start the worker fleet consumer
startWorker();
