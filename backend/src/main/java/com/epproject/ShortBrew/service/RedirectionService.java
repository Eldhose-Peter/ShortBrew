package com.epproject.ShortBrew.service;

import com.epproject.ShortBrew.exception.GoneException;
import com.epproject.ShortBrew.exception.NotFoundException;
import com.epproject.ShortBrew.model.Url;
import com.epproject.ShortBrew.repository.UrlRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class RedirectionService {

    private final UrlRepository urlRepository;
    private final UrlCacheService urlCacheService;

    public RedirectionService(UrlRepository urlRepository, UrlCacheService urlCacheService) {
        this.urlRepository = urlRepository;
        this.urlCacheService = urlCacheService;
    }

    /**
     * Resolves the target URL for a given short code or custom alias.
     * Uses cache-aside pattern with Redis validation.
     *
     * @param code the short code or custom alias
     * @return the target URL to redirect to
     * @throws NotFoundException if the code does not exist
     * @throws GoneException if the URL is deactivated or expired
     */
    public String resolveRedirect(String code) {
        // Cache-aside: check Redis first (validated internally)
        String cachedTargetUrl = urlCacheService.getValidatedTargetUrl(code);
        if (cachedTargetUrl != null) {
            return cachedTargetUrl;
        }

        // Postgres on miss
        Url url = urlRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("URL not found"));

        // Validate active and expiry
        if (!url.isActive() || (url.expiresAt() != null && url.expiresAt().isBefore(Instant.now()))) {
            urlCacheService.evict(code);
            throw new GoneException("URL has been deactivated or has expired");
        }

        // Repopulate cache
        urlCacheService.put(code, url);

        return url.targetUrl();
    }
}
