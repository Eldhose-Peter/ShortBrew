import { AsyncLocalStorage } from 'async_hooks';

export interface LogContext {
  request_id?: string;
  trace_id?: string;
  span_id?: string;
  [key: string]: any;
}

export const logContextStorage = new AsyncLocalStorage<LogContext>();

/**
 * Runs an asynchronous or synchronous function within a given log context (AsyncLocalStorage).
 */
export function runWithContext<T>(context: LogContext, fn: () => Promise<T> | T): Promise<T> | T {
  return logContextStorage.run(context, fn);
}

/**
 * Gets the current store from AsyncLocalStorage.
 */
export function getLogContext(): LogContext {
  return logContextStorage.getStore() || {};
}
