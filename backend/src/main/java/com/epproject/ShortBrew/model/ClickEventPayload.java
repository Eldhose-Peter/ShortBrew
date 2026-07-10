package com.epproject.ShortBrew.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ClickEventPayload(
    @JsonProperty("url_id") Long urlId,
    @JsonProperty("short_code") String shortCode,
    @JsonProperty("referrer") String referrer,
    @JsonProperty("user_agent") String userAgent,
    @JsonProperty("ip_hash") String ipHash,
    @JsonProperty("clicked_at") String clickedAt,
    @JsonProperty("retry_count") int retryCount
) {}
