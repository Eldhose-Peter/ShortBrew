package com.epproject.ShortBrew.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record URLAnalyticsResponse(
    @JsonProperty("short_code") String shortCode,
    @JsonProperty("total_clicks") long totalClicks,
    @JsonProperty("daily_clicks") List<DailyClickPoint> dailyClicks,
    @JsonProperty("top_referrers") List<ReferrerStat> topReferrers,
    @JsonProperty("top_countries") List<CountryStat> topCountries
) {}
