package com.epproject.ShortBrew.controller;

import com.epproject.ShortBrew.controller.dto.UrlCreateRequest;
import com.epproject.ShortBrew.controller.dto.UrlPageResponse;
import com.epproject.ShortBrew.controller.dto.UrlResponse;
import com.epproject.ShortBrew.controller.dto.UrlUpdateRequest;
import com.epproject.ShortBrew.model.Url;
import com.epproject.ShortBrew.model.User;
import com.epproject.ShortBrew.repository.UrlRepository.UrlSearchResult;
import com.epproject.ShortBrew.security.CurrentUser;
import com.epproject.ShortBrew.security.RequireAuth;
import com.epproject.ShortBrew.service.UrlService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/urls")
@RequireAuth
public class UrlController {

    private final UrlService urlService;
    private final String baseUrl;

    public UrlController(
            UrlService urlService,
            @Value("${shortbrew.base-url:http://localhost:8080/}") String baseUrl
    ) {
        this.urlService = urlService;
        this.baseUrl = baseUrl;
    }

    @PostMapping
    public ResponseEntity<UrlResponse> createUrl(
            @RequestBody UrlCreateRequest request,
            @CurrentUser User currentUser
    ) {
        Url url = urlService.createUrl(
            request.targetUrl(),
            request.customAlias(),
            request.expiresAt(),
            request.title(),
            currentUser.id()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(url));
    }

    @GetMapping
    public ResponseEntity<UrlPageResponse> listUrls(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "10") int pageSize,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "is_active", required = false) Boolean isActive,
            @CurrentUser User currentUser
    ) {
        UrlSearchResult result = urlService.searchUrls(currentUser.id(), page, pageSize, search, isActive);
        List<UrlResponse> items = result.items().stream()
                .map(this::mapToResponse)
                .toList();

        int totalPages = result.total() == 0 ? 0 : (int) ((result.total() + pageSize - 1) / pageSize);

        UrlPageResponse response = new UrlPageResponse(
            items,
            result.total(),
            page,
            pageSize,
            totalPages
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{url_id}")
    public ResponseEntity<UrlResponse> getUrl(
            @PathVariable("url_id") Long urlId,
            @CurrentUser User currentUser
    ) {
        Url url = urlService.getUrlByIdAndOwner(urlId, currentUser.id());
        return ResponseEntity.ok(mapToResponse(url));
    }

    @PatchMapping("/{url_id}")
    public ResponseEntity<UrlResponse> updateUrl(
            @PathVariable("url_id") Long urlId,
            @RequestBody UrlUpdateRequest request,
            @CurrentUser User currentUser
    ) {
        Url url = urlService.updateUrl(
            urlId,
            request.targetUrl(),
            request.expiresAt(),
            request.isActive(),
            request.title(),
            currentUser.id()
        );
        return ResponseEntity.ok(mapToResponse(url));
    }

    @DeleteMapping("/{url_id}")
    public ResponseEntity<Void> deleteUrl(
            @PathVariable("url_id") Long urlId,
            @CurrentUser User currentUser
    ) {
        urlService.deleteUrl(urlId, currentUser.id());
        return ResponseEntity.noContent().build();
    }

    private UrlResponse mapToResponse(Url url) {
        String effectiveCode = url.getEffectiveCode();
        String formattedBaseUrl = baseUrl;
        if (!formattedBaseUrl.endsWith("/")) {
            formattedBaseUrl += "/";
        }
        String shortUrl = formattedBaseUrl + effectiveCode;
        return new UrlResponse(
            url.id(),
            url.shortCode(),
            url.customAlias(),
            url.targetUrl(),
            url.title(),
            url.createdAt(),
            url.expiresAt(),
            url.isActive(),
            url.totalClicks(),
            shortUrl
        );
    }
}
