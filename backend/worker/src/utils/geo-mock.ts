import { createHash } from 'crypto';

const MOCK_COUNTRIES = ["US", "GB", "DE", "IN", "BR", "JP", "CA", "AU", "FR", "SG"];

/**
 * Deterministically pseudo-geolocates a country code from the hashed IP address.
 */
export function mockCountryForIpHash(ipHash?: string): string {
  if (!ipHash) {
    return "XX";
  }
  const hash = createHash('sha256').update(ipHash).digest('hex');
  const num = BigInt("0x" + hash);
  const index = Number(num % BigInt(MOCK_COUNTRIES.length));
  return MOCK_COUNTRIES[index];
}
