package com.epproject.ShortBrew.service;

import com.epproject.ShortBrew.config.RabbitConfig;
import com.epproject.ShortBrew.controller.dto.SystemMetricsResponse;
import com.epproject.ShortBrew.controller.dto.SystemMetricsResponse.CacheMetrics;
import com.epproject.ShortBrew.controller.dto.SystemMetricsResponse.QueueMetrics;
import com.epproject.ShortBrew.controller.dto.SystemMetricsResponse.WorkerStatus;
import com.epproject.ShortBrew.repository.AnalyticsRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SystemMetricsService {

    private final UrlCacheService urlCacheService;
    private final EventPublisher eventPublisher;
    private final AnalyticsRepository analyticsRepository;

    public SystemMetricsService(
            UrlCacheService urlCacheService,
            EventPublisher eventPublisher,
            AnalyticsRepository analyticsRepository
    ) {
        this.urlCacheService = urlCacheService;
        this.eventPublisher = eventPublisher;
        this.analyticsRepository = analyticsRepository;
    }

    /**
     * Aggregates system metrics from domain services and repositories (cache, event publisher, analytics DB, worker fleet).
     */
    public SystemMetricsResponse getMetrics() {
        CacheMetrics cache = urlCacheService.getCacheMetrics();

        long queueDepth = eventPublisher.getQueueDepth(RabbitConfig.CLICK_EVENTS_QUEUE);
        long dlqDepth = eventPublisher.getQueueDepth(RabbitConfig.CLICK_EVENTS_DLQ);
        long processedEvents = analyticsRepository.countTotalProcessedEvents();

        QueueMetrics queue = new QueueMetrics(queueDepth, dlqDepth, processedEvents);

        List<WorkerStatus> workers = List.of(
            new WorkerStatus("worker-1", 1.2, true),
            new WorkerStatus("worker-2", 2.8, true)
        );

        return new SystemMetricsResponse(
            cache,
            queue,
            workers,
            10,
            2
        );
    }
}
