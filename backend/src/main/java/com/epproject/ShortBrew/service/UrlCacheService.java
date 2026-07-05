package com.epproject.ShortBrew.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class UrlCacheService {

    private final StringRedisTemplate redisTemplate;

    public UrlCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String get(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return redisTemplate.opsForValue().get(getCacheKey(code));
    }

    public void put(String code, String targetUrl, boolean isActive, Instant expiresAt) {
        String key = getCacheKey(code);
        if (!isActive || (expiresAt != null && expiresAt.isBefore(Instant.now()))) {
            evict(code);
        } else {
            if (expiresAt != null) {
                long ttlSeconds = Duration.between(Instant.now(), expiresAt).toSeconds();
                if (ttlSeconds > 0) {
                    redisTemplate.opsForValue().set(key, targetUrl, Duration.ofSeconds(ttlSeconds));
                } else {
                    evict(code);
                }
            } else {
                redisTemplate.opsForValue().set(key, targetUrl);
            }
        }
    }

    public void evict(String code) {
        if (code != null && !code.isBlank()) {
            redisTemplate.delete(getCacheKey(code));
        }
    }

    private String getCacheKey(String code) {
        return "url:" + code;
    }
}
