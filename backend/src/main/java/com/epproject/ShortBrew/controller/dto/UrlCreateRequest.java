package com.epproject.ShortBrew.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record UrlCreateRequest(
    @JsonProperty("target_url") String targetUrl,
    @JsonProperty("custom_alias") String customAlias,
    @JsonProperty("expires_at") Instant expiresAt,
    String title
) {}
