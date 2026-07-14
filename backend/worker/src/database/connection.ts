import { Pool } from 'pg';
import { settings } from '../config/settings';

export const dbPool = new Pool({
  host: settings.db.host,
  port: settings.db.port,
  user: settings.db.user,
  password: settings.db.password,
  database: settings.db.database,
  max: settings.db.poolSize,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 2000,
});
