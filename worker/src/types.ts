export interface ClickEvent {
  url_id: number;
  short_code: string;
  referrer?: string;
  user_agent?: string;
  ip_hash?: string;
  clicked_at: string;
  retry_count?: number;
  request_id?: string;
  trace_id?: string;
  span_id?: string;
}
