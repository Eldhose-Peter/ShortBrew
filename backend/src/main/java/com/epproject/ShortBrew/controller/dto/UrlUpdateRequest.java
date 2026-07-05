package com.epproject.ShortBrew.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record UrlUpdateRequest(
    @JsonProperty("target_url") String targetUrl,
    @JsonProperty("expires_at") Instant expiresAt,
    @JsonProperty("is_active") Boolean isActive,
    String title
) {}
