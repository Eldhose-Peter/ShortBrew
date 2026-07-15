package com.epproject.ShortBrew.service;

import com.epproject.ShortBrew.controller.dto.*;
import com.epproject.ShortBrew.model.Url;
import com.epproject.ShortBrew.repository.AnalyticsRepository;
import com.epproject.ShortBrew.repository.UrlRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AnalyticsService {

    private final UrlService urlService;
    private final AnalyticsRepository analyticsRepository;
    private final UrlRepository urlRepository;

    public AnalyticsService(UrlService urlService, AnalyticsRepository analyticsRepository, UrlRepository urlRepository) {
        this.urlService = urlService;
        this.analyticsRepository = analyticsRepository;
        this.urlRepository = urlRepository;
    }

    public URLAnalyticsResponse getUrlAnalytics(Long urlId, UUID userId, int days) {
        // Validate URL ownership & existence - throws NotFoundException if not matching
        Url url = urlService.getUrlByIdAndOwner(urlId, userId);

        List<DailyClickPoint> daily = analyticsRepository.dailyClicksForUrl(urlId, days);
        List<ReferrerStat> referrers = analyticsRepository.referrerBreakdown(urlId, days);
        List<CountryStat> countries = analyticsRepository.countryBreakdown(urlId, days);

        return new URLAnalyticsResponse(
            url.getEffectiveCode(),
            url.totalClicks(),
            daily,
            referrers,
            countries
        );
    }

    public UserAnalyticsSummary getUserSummary(UUID userId) {
        long totalUrls = urlRepository.totalUrlsForOwner(userId);
        long totalClicks = urlRepository.totalClicksForOwner(userId);
        long clicksToday = analyticsRepository.clicksTodayForOwner(userId);
        List<TopURLEntry> topUrls = urlRepository.topUrlsForOwner(userId, 5);
        List<DailyClickPoint> dailyClicks = analyticsRepository.dailyClicksForOwner(userId, 14);

        return new UserAnalyticsSummary(
            totalUrls,
            totalClicks,
            clicksToday,
            topUrls,
            dailyClicks
        );
    }
}
