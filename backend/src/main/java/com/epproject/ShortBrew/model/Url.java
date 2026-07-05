package com.epproject.ShortBrew.model;

import java.time.Instant;
import java.util.UUID;

public record Url(
    Long id,
    String shortCode,
    String customAlias,
    String targetUrl,
    String title,
    UUID ownerId,
    Instant createdAt,
    Instant expiresAt,
    boolean isActive,
    long totalClicks
) {
    public String getEffectiveCode() {
        return (customAlias != null && !customAlias.isBlank()) ? customAlias : shortCode;
    }
}
