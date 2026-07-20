package com.epproject.ShortBrew.service;

import com.epproject.ShortBrew.controller.dto.SystemMetricsResponse.CacheMetrics;
import com.epproject.ShortBrew.controller.dto.SystemMetricsResponse.WorkerStatus;
import com.epproject.ShortBrew.exception.GoneException;
import com.epproject.ShortBrew.model.Url;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Service
public class UrlCacheService {

    private static final Logger log = LoggerFactory.getLogger(UrlCacheService.class);

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

    public UrlCachePayload getValidatedUrl(String code) {
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
            return payload;
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

    public CacheMetrics getCacheMetrics() {
        try {
            Properties stats = redisTemplate.execute((RedisCallback<Properties>) connection -> connection.serverCommands().info("stats"));
            if (stats != null) {
                long hits = parseLongOrDefault(stats.getProperty("keyspace_hits"), 0L);
                long misses = parseLongOrDefault(stats.getProperty("keyspace_misses"), 0L);
                long total = hits + misses;
                double hitRate = total > 0 ? (double) hits / total : 0.0;
                return new CacheMetrics(hits, misses, hitRate);
            }
        } catch (Exception e) {
            log.debug("Could not fetch Redis cache metrics: {}", e.getMessage());
        }
        return new CacheMetrics(0L, 0L, 0.0);
    }

    public List<WorkerStatus> getWorkerFleetStatus() {
        try {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries("worker:heartbeats");
            if (entries != null && !entries.isEmpty()) {
                long now = System.currentTimeMillis();
                List<WorkerStatus> workers = new ArrayList<>();
                for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                    String workerId = (String) entry.getKey();
                    try {
                        long lastPing = Long.parseLong((String) entry.getValue());
                        double secondsAgo = Math.round((now - lastPing) / 100.0) / 10.0;
                        boolean alive = secondsAgo <= 15.0;
                        workers.add(new WorkerStatus(workerId, secondsAgo, alive));
                    } catch (Exception ignored) {
                    }
                }
                if (!workers.isEmpty()) {
                    return workers;
                }
            }
        } catch (Exception e) {
            log.debug("Could not fetch worker heartbeats from Redis: {}", e.getMessage());
        }
        return List.of();
    }

    private long parseLongOrDefault(String value, long defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String getCacheKey(String code) {
        return "url:" + code;
    }
}
