package com.epproject.ShortBrew.service;

import org.springframework.stereotype.Service;

/**
 * ShortCodeGenerator implements a bijective permutation and Base62 encoding flow
 * to generate short codes for URLs.
 *
 * --- LOGIC & ALGORITHM ---
 * 1. Offsetting: We add SHORT_CODE_ID_OFFSET to the database ID. This guarantees that even
 *    the first database row (ID=1) starts at a large number (e.g. 10 billion), resulting
 *    in consistent, professional-looking short code lengths (e.g. 6+ characters).
 * 2. Permutation (permuteId): We pass the offset ID through a bijective 64-bit finalizer
 *    (MurmurHash3's fmix64). Because each step is mathematically invertible (coprime multiplications
 *    and XOR shifts), it is a bijection (one-to-one), guaranteeing ZERO collisions. The avalanching
 *    effect shuffles sequential inputs (1, 2, 3...) to produce non-sequential, random-looking values,
 *    preventing simple link enumeration.
 * 3. Base62 Encoding (base62Encode): We encode the 64-bit unsigned permuted value using a URL-safe
 *    alphanumeric character set (0-9, a-z, A-Z), producing a short, compact URL slug.
 *
 * --- DRAWBACKS & CONSIDERATIONS ---
 * 1. Two-Step Database Operations: Since we require the auto-incremented database ID (url.id)
 *    to generate the short code, we must write a row first to get the ID, compute the short code,
 *    and then update the row with the generated code. This doubles the write load per URL creation.
 *    (Alternative: Pre-fetching the sequence's next value via 'SELECT nextval' is possible but adds a roundtrip).
 * 2. Security by Obfuscation Only: The permutation function is deterministic and reversible.
 *    An attacker who knows the algorithm (fmix64) and alphabet can decode a short code back to its
 *    original database ID. It prevents casual scanning but is not cryptographically secure against
 *    targeted sequence extraction.
 * 3. Scalability Bottleneck: Relying on a centralized database sequence sequence limits the ability
 *    to scale the application horizontally across sharded or distributed databases (unlike Snowflake
 *    IDs or UUIDs).
 */
@Service
public class ShortCodeGenerator {

    private static final long SHORT_CODE_ID_OFFSET = 10000000000L;
    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /**
     * Generates a short code from a unique database ID.
     * url.short_code = base62_encode(permute_id(url.id + SHORT_CODE_ID_OFFSET))
     */
    public String generate(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        long permuted = permuteId(id + SHORT_CODE_ID_OFFSET);
        return base62Encode(permuted);
    }

    private static long permuteId(long id) {
        long x = id;
        x ^= (x >>> 30);
        x *= 0xbf58476d1ce4e5b9L;
        x ^= (x >>> 27);
        x *= 0x94d049bb133111ebL;
        x ^= (x >>> 31);
        return x;
    }

    private static String base62Encode(long value) {
        StringBuilder sb = new StringBuilder();
        long temp = value;
        if (temp == 0) {
            return "0";
        }
        while (temp != 0) {
            long remainder = Long.remainderUnsigned(temp, 62);
            sb.append(ALPHABET.charAt((int) remainder));
            temp = Long.divideUnsigned(temp, 62);
        }
        return sb.reverse().toString();
    }
}
