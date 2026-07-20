package com.epproject.ShortBrew.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record UrlCachePayload(
    @JsonProperty("url_id") Long urlId,
    @JsonProperty("target_url") String targetUrl,
    @JsonProperty("owner_id") UUID ownerId,
    @JsonProperty("is_active") boolean isActive,
    @JsonProperty("expires_at") String expiresAt
) {}
