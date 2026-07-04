package com.epproject.ShortBrew.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
    UUID id,
    String email,
    @JsonProperty("full_name") String fullName,
    @JsonProperty("is_active") boolean isActive,
    @JsonProperty("created_at") Instant createdAt
) {}
