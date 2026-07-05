package com.epproject.ShortBrew.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record UrlResponse(
    Long id,
    @JsonProperty("short_code") String shortCode,
    @JsonProperty("custom_alias") String customAlias,
    @JsonProperty("target_url") String targetUrl,
    String title,
    @JsonProperty("created_at") Instant createdAt,
    @JsonProperty("expires_at") Instant expiresAt,
    @JsonProperty("is_active") boolean isActive,
    @JsonProperty("total_clicks") long totalClicks,
    @JsonProperty("short_url") String shortUrl
) {}
