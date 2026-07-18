import { getLogContext } from './context';

export type LogLevel = 'debug' | 'info' | 'warn' | 'error';

const LOG_LEVEL_PRIORITY: Record<LogLevel, number> = {
  debug: 10,
  info: 20,
  warn: 30,
  error: 40,
};

function getMinLogLevel(): number {
  const envLevel = (process.env.LOG_LEVEL || '').toLowerCase() as LogLevel;
  if (envLevel in LOG_LEVEL_PRIORITY) {
    return LOG_LEVEL_PRIORITY[envLevel];
  }
  return process.env.NODE_ENV === 'development' ? LOG_LEVEL_PRIORITY.debug : LOG_LEVEL_PRIORITY.info;
}

function isJsonFormat(): boolean {
  const format = (process.env.LOG_FORMAT || '').toLowerCase();
  if (format === 'console' || format === 'pretty' || format === 'text') {
    return false;
  }
  if (format === 'json') {
    return true;
  }
  // Default: JSON in non-debug/production environments
  return (process.env.LOG_LEVEL || '').toLowerCase() !== 'debug' && process.env.NODE_ENV !== 'development';
}

function formatLog(level: LogLevel, message: string, meta?: Record<string, any>): string {
  const timestamp = new Date().toISOString();
  const context = getLogContext();
  const mergedMeta = { ...context, ...meta };

  if (isJsonFormat()) {
    const logObj: Record<string, any> = {
      timestamp,
      level,
      message,
      service: 'worker',
      ...mergedMeta,
    };
    return JSON.stringify(logObj);
  } else {
    const reqIdStr = mergedMeta.request_id ? ` [request_id=${mergedMeta.request_id}]` : '';
    const metaStr = Object.keys(mergedMeta).length > 0
      ? ' ' + JSON.stringify(mergedMeta)
      : '';
    return `[${timestamp}] ${level.toUpperCase()}${reqIdStr}: ${message}${metaStr}`;
  }
}

function shouldLog(level: LogLevel): boolean {
  return LOG_LEVEL_PRIORITY[level] >= getMinLogLevel();
}

export const logger = {
  debug(message: string, meta?: Record<string, any>): void {
    if (shouldLog('debug')) {
      console.log(formatLog('debug', message, meta));
    }
  },

  info(message: string, meta?: Record<string, any>): void {
    if (shouldLog('info')) {
      console.log(formatLog('info', message, meta));
    }
  },

  warn(message: string, meta?: Record<string, any>): void {
    if (shouldLog('warn')) {
      console.warn(formatLog('warn', message, meta));
    }
  },

  error(message: string, error?: unknown, meta?: Record<string, any>): void {
    if (shouldLog('error')) {
      let errDetails: Record<string, any> = {};
      if (error instanceof Error) {
        errDetails = { error_message: error.message, stack: error.stack };
      } else if (error != null) {
        errDetails = { error: String(error) };
      }
      console.error(formatLog('error', message, { ...errDetails, ...meta }));
    }
  },
};
