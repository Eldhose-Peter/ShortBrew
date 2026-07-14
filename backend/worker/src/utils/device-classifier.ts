/**
 * Classifies the device type based on the User-Agent header value.
 */
export function classifyDevice(userAgent?: string): string {
  if (!userAgent) return 'Desktop';
  const ua = userAgent.toLowerCase();
  if (ua.includes('mobile') || ua.includes('android') || ua.includes('iphone') || ua.includes('ipad')) {
    return 'Mobile';
  }
  if (ua.includes('tablet')) {
    return 'Tablet';
  }
  return 'Desktop';
}
