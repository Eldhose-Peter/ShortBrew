package com.epproject.ShortBrew.controller;

import com.epproject.ShortBrew.exception.GoneException;
import com.epproject.ShortBrew.exception.NotFoundException;
import com.epproject.ShortBrew.model.Url;
import com.epproject.ShortBrew.repository.UrlRepository;
import com.epproject.ShortBrew.service.UrlCacheService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
public class RedirectController {

    private final UrlRepository urlRepository;
    private final UrlCacheService urlCacheService;

    public RedirectController(UrlRepository urlRepository, UrlCacheService urlCacheService) {
        this.urlRepository = urlRepository;
        this.urlCacheService = urlCacheService;
    }

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable("code") String code) {
        // Cache-aside: check Redis first
        String cachedTargetUrl = urlCacheService.get(code);
        if (cachedTargetUrl != null) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, cachedTargetUrl)
                    .build();
        }

        // Postgres on miss
        Url url = urlRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("URL not found"));

        // Check if deactivated or past expires_at
        if (!url.isActive() || (url.expiresAt() != null && url.expiresAt().isBefore(Instant.now()))) {
            // Evict from cache just in case
            urlCacheService.evict(code);
            throw new GoneException("URL has been deactivated or has expired");
        }

        // Repopulate cache
        urlCacheService.put(code, url.targetUrl(), url.isActive(), url.expiresAt());

        // 302 Redirect
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, url.targetUrl())
                .build();
    }
}
