package com.epproject.ShortBrew.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record UserAnalyticsSummary(
    @JsonProperty("total_urls") long totalUrls,
    @JsonProperty("total_clicks") long totalClicks,
    @JsonProperty("clicks_today") long clicksToday,
    @JsonProperty("top_urls") List<TopURLEntry> topUrls,
    @JsonProperty("daily_clicks") List<DailyClickPoint> dailyClicks
) {}
