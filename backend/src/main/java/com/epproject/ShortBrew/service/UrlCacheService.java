package com.epproject.ShortBrew.service;

import com.epproject.ShortBrew.exception.GoneException;
import com.epproject.ShortBrew.model.Url;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class UrlCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final int cacheTtlSeconds;

    public UrlCacheService(
            StringRedisTemplate redisTemplate,
            @org.springframework.beans.factory.annotation.Value("${URL_CACHE_TTL_SECONDS:3600}") int cacheTtlSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public String getValidatedTargetUrl(String code) {
        String json = redisTemplate.opsForValue().get(getCacheKey(code));
        if (json == null) {
            return null;
        }
        try {
            UrlCachePayload payload = objectMapper.readValue(json, UrlCachePayload.class);
            // Validate active
            if (!payload.isActive()) {
                evict(code);
                throw new GoneException("URL has been deactivated");
            }
            // Validate expiry
            if (payload.expiresAt() != null) {
                Instant expiresAt = Instant.parse(payload.expiresAt());
                if (expiresAt.isBefore(Instant.now())) {
                    evict(code);
                    throw new GoneException("URL has expired");
                }
            }
            return payload.targetUrl();
        } catch (GoneException e) {
            throw e;
        } catch (Exception e) {
            // In case of parsing error, treat as miss
            return null;
        }
    }

    public void put(String code, Url url) {
        String key = getCacheKey(code);
        if (!url.isActive() || (url.expiresAt() != null && url.expiresAt().isBefore(Instant.now()))) {
            evict(code);
        } else {
            long ttlSeconds = cacheTtlSeconds;
            if (url.expiresAt() != null) {
                long remainingSeconds = Duration.between(Instant.now(), url.expiresAt()).toSeconds();
                if (remainingSeconds <= 0) {
                    evict(code);
                    return;
                }
                ttlSeconds = Math.min(remainingSeconds, cacheTtlSeconds);
            }
            try {
                String expiresAtStr = url.expiresAt() != null ? url.expiresAt().toString() : null;
                UrlCachePayload payload = new UrlCachePayload(
                    url.id(),
                    url.targetUrl(),
                    url.ownerId(),
                    url.isActive(),
                    expiresAtStr
                );
                String json = objectMapper.writeValueAsString(payload);
                redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(ttlSeconds));
            } catch (Exception e) {
                // If serialization fails, do not write to Redis
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
