package com.epproject.ShortBrew.service;

import com.epproject.ShortBrew.exception.ConflictException;
import com.epproject.ShortBrew.exception.NotFoundException;
import com.epproject.ShortBrew.exception.ValidationException;
import com.epproject.ShortBrew.model.Url;
import com.epproject.ShortBrew.repository.UrlRepository;
import com.epproject.ShortBrew.repository.UrlRepository.UrlSearchResult;
import com.epproject.ShortBrew.service.UrlCacheService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class UrlService {

    private final UrlRepository urlRepository;
    private final UrlCacheService urlCacheService;
    private final ShortCodeGenerator shortCodeGenerator;

    public UrlService(
            UrlRepository urlRepository,
            UrlCacheService urlCacheService,
            ShortCodeGenerator shortCodeGenerator
    ) {
        this.urlRepository = urlRepository;
        this.urlCacheService = urlCacheService;
        this.shortCodeGenerator = shortCodeGenerator;
    }

    @Transactional
    public Url createUrl(String targetUrl, String customAlias, Instant expiresAt, String title, UUID ownerId) {
        validateTargetUrl(targetUrl);
        validateCustomAlias(customAlias);

        if (customAlias != null && !customAlias.isBlank()) {
            if (urlRepository.findByCustomAlias(customAlias).isPresent() || urlRepository.findByShortCode(customAlias).isPresent()) {
                throw new ConflictException("Custom alias is already taken");
            }
        }

        Url url = new Url(
            null,
            null,
            customAlias,
            targetUrl,
            title,
            ownerId,
            null,
            expiresAt,
            true,
            0L
        );

        Url savedWithoutShortCode = urlRepository.save(url);

        String shortCode = shortCodeGenerator.generate(savedWithoutShortCode.id());

        Url saved = urlRepository.updateShortCode(savedWithoutShortCode.id(), shortCode);

        // Repopulate cache for new effective code
        urlCacheService.put(saved.getEffectiveCode(), saved);

        return saved;
    }

    public Url getUrlByIdAndOwner(Long id, UUID ownerId) {
        Url url = urlRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("URL not found"));
        if (!url.ownerId().equals(ownerId)) {
            throw new NotFoundException("URL not found");
        }
        return url;
    }

    public UrlSearchResult searchUrls(UUID ownerId, int page, int pageSize, String search, Boolean isActive) {
        if (page < 1) {
            page = 1;
        }
        if (pageSize < 1) {
            pageSize = 10;
        } else if (pageSize > 100) {
            pageSize = 100;
        }
        return urlRepository.search(ownerId, page, pageSize, search, isActive);
    }

    public Url updateUrl(Long id, String targetUrl, Instant expiresAt, Boolean isActive, String title, UUID ownerId) {
        Url existing = urlRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("URL not found"));
        if (!existing.ownerId().equals(ownerId)) {
            throw new NotFoundException("URL not found");
        }

        if (targetUrl != null) {
            validateTargetUrl(targetUrl);
        }

        Url updated = new Url(
            existing.id(),
            existing.shortCode(),
            existing.customAlias(),
            targetUrl != null ? targetUrl : existing.targetUrl(),
            title != null ? title : existing.title(),
            existing.ownerId(),
            existing.createdAt(),
            expiresAt != null ? expiresAt : existing.expiresAt(),
            isActive != null ? isActive : existing.isActive(),
            existing.totalClicks()
        );

        String oldEffectiveCode = existing.getEffectiveCode();
        Url saved = urlRepository.update(updated);
        String newEffectiveCode = saved.getEffectiveCode();

        // Invalidate old and new effective code cache entries
        urlCacheService.evict(oldEffectiveCode);
        urlCacheService.evict(newEffectiveCode);

        // Repopulate Redis cache entry for the new effective code if active
        if (saved.isActive()) {
            urlCacheService.put(newEffectiveCode, saved);
        }

        return saved;
    }

    public void deleteUrl(Long id, UUID ownerId) {
        Url existing = urlRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("URL not found"));
        if (!existing.ownerId().equals(ownerId)) {
            throw new NotFoundException("URL not found");
        }

        String effectiveCode = existing.getEffectiveCode();
        // Invalidate cache first
        urlCacheService.evict(effectiveCode);

        urlRepository.delete(id);
    }

    private void validateTargetUrl(String targetUrl) {
        if (targetUrl == null || (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://"))) {
            throw new ValidationException("target_url must start with http:// or https://");
        }
    }

    private void validateCustomAlias(String customAlias) {
        if (customAlias != null && !customAlias.isBlank()) {
            if (customAlias.length() < 3 || customAlias.length() > 64) {
                throw new ValidationException("custom_alias must be between 3 and 64 characters");
            }
            if (!customAlias.matches("^[a-zA-Z0-9_-]+$")) {
                throw new ValidationException("custom_alias must be alphanumeric");
            }
        }
    }

}
