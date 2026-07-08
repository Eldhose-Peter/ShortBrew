package com.epproject.ShortBrew.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<List> rateLimitScript;

    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setResultType(List.class);
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/rate_limiter.lua")));
        this.rateLimitScript = script;
    }

    /**
     * Evaluates if a request is allowed under the given key and limit configuration.
     *
     * @param key      the rate limiting Redis key (e.g. rate_limit:create:userId)
     * @param limit    the limit of requests in the window
     * @param windowMs the sliding window size in milliseconds
     * @return true if the request is allowed, false if it is rate-limited
     */
    public boolean isAllowed(String key, int limit, long windowMs) {
        long now = System.currentTimeMillis();
        try {
            List<?> result = redisTemplate.execute(
                    rateLimitScript,
                    Collections.singletonList(key),
                    String.valueOf(now),
                    String.valueOf(windowMs),
                    String.valueOf(limit)
            );
            if (result != null && !result.isEmpty()) {
                Number allowed = (Number) result.get(0);
                return allowed.intValue() == 1;
            }
        } catch (Exception e) {
            // Fail-open on Redis errors to prevent API downtime
        }
        return true;
    }
}
