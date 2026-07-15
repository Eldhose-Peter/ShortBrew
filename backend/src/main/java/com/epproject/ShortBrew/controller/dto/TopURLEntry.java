package com.epproject.ShortBrew.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TopURLEntry(
    @JsonProperty("short_code") String shortCode,
    @JsonProperty("target_url") String targetUrl,
    String title,
    @JsonProperty("total_clicks") long totalClicks
) {}
