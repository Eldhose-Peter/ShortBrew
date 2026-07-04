package com.epproject.ShortBrew.model;

import java.time.Instant;
import java.util.UUID;

public record User(
    UUID id,
    String email,
    String hashedPassword,
    String fullName,
    boolean isActive,
    Instant createdAt
) {}
