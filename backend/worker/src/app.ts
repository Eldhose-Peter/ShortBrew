import * as dotenv from 'dotenv';
import { startWorker } from './event-consumer';
import { pool } from './db';

// Load environment variables (from .env or local environment)
dotenv.config();

// Handle termination gracefully
const shutdown = async () => {
  console.log('Shutting down. Closing database pool...');
  try {
    await pool.end();
    console.log('Database pool closed successfully.');
    process.exit(0);
  } catch (err) {
    console.error('Error closing database pool:', err);
    process.exit(1);
  }
};

process.on('SIGTERM', shutdown);
process.on('SIGINT', shutdown);

// Start the worker
startWorker();
