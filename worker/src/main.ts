import { startWorker } from './messaging/consumer';
import { dbPool } from './database/connection';

const shutdown = async () => {
  console.log('Shutting down. Closing database pool...');
  try {
    await dbPool.end();
    console.log('Database pool closed successfully.');
    process.exit(0);
  } catch (err) {
    console.error('Error closing database pool:', err);
    process.exit(1);
  }
};

process.on('SIGTERM', shutdown);
process.on('SIGINT', shutdown);

// Start the worker fleet consumer
startWorker();
