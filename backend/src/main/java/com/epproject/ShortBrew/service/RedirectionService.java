package com.epproject.ShortBrew.service;

import com.epproject.ShortBrew.exception.GoneException;
import com.epproject.ShortBrew.exception.NotFoundException;
import com.epproject.ShortBrew.model.Url;
import com.epproject.ShortBrew.model.UrlCachePayload;
import com.epproject.ShortBrew.repository.UrlRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

@Service
public class RedirectionService {

    private static final Logger logger = LoggerFactory.getLogger(RedirectionService.class);

    private final UrlRepository urlRepository;
    private final RedisService redisService;
    private final RabbitMQService rabbitMQService;

    public RedirectionService(
            UrlRepository urlRepository,
            RedisService redisService,
            RabbitMQService rabbitMQService
    ) {
        this.urlRepository = urlRepository;
        this.redisService = redisService;
        this.rabbitMQService = rabbitMQService;
    }

    /**
     * Resolves the target URL for a given short code or custom alias, and publishes a click event.
     * Uses cache-aside pattern with Redis validation.
     *
     * @param code the short code or custom alias
     * @param request the HTTP request triggering the redirect
     * @return the resolved URL details
     * @throws NotFoundException if the code does not exist
     * @throws GoneException if the URL is deactivated or expired
     */
    public String resolveRedirect(String code, HttpServletRequest request) {
        // Cache-aside: check Redis first (validated internally)
        UrlCachePayload cachedUrl = redisService.getValidatedUrl(code);
        if (cachedUrl != null) {
            publishClickEvent(cachedUrl.urlId(), code, request);
            return cachedUrl.targetUrl();
        }

        // Postgres on miss
        Url url = urlRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("URL not found"));

        // Validate active and expiry
        if (!url.isActive() || (url.expiresAt() != null && url.expiresAt().isBefore(Instant.now()))) {
            redisService.evict(code);
            throw new GoneException("URL has been deactivated or has expired");
        }

        // Repopulate cache
        redisService.put(code, url);

        publishClickEvent(url.id(), code, request);
        return url.targetUrl();
    }

    private void publishClickEvent(Long urlId, String shortCode, HttpServletRequest request) {
        try {
            String referrer = request.getHeader("Referer");
            if (referrer == null) {
                referrer = request.getHeader("Referrer");
            }
            String userAgent = request.getHeader("User-Agent");
            String clientIp = getClientIp(request);
            String ipHash = hashIp(clientIp);

            rabbitMQService.publishClickEvent(
                    urlId,
                    shortCode,
                    referrer,
                    userAgent,
                    ipHash
            );
        } catch (Exception e) {
            logger.error("Failed to prepare or publish click event for URL ID: {}, shortCode: {}", urlId, shortCode, e);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            return request.getRemoteAddr();
        }
        return ip.split(",")[0].trim();
    }

    private String hashIp(String ip) {
        if (ip == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(ip.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
