package com.epproject.ShortBrew.service;

import com.epproject.ShortBrew.config.RabbitConfig;
import com.epproject.ShortBrew.controller.dto.SystemMetricsResponse;
import com.epproject.ShortBrew.controller.dto.SystemMetricsResponse.CacheMetrics;
import com.epproject.ShortBrew.controller.dto.SystemMetricsResponse.QueueMetrics;
import com.epproject.ShortBrew.controller.dto.SystemMetricsResponse.WorkerStatus;
import com.epproject.ShortBrew.repository.AnalyticsRepository;
import com.epproject.ShortBrew.repository.DatabaseRepository;
import com.epproject.ShortBrew.repository.DatabaseRepository.DbPoolMetrics;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SystemMetricsService {

    private final RedisService redisService;
    private final RabbitMQService rabbitMQService;
    private final AnalyticsRepository analyticsRepository;
    private final DatabaseRepository databaseRepository;

    public SystemMetricsService(
            RedisService redisService,
            RabbitMQService rabbitMQService,
            AnalyticsRepository analyticsRepository,
            DatabaseRepository databaseRepository
    ) {
        this.redisService = redisService;
        this.rabbitMQService = rabbitMQService;
        this.analyticsRepository = analyticsRepository;
        this.databaseRepository = databaseRepository;
    }

    /**
     * Aggregates system metrics from domain services and repositories (Redis, RabbitMQ, analytics DB, DB pool, worker fleet).
     */
    public SystemMetricsResponse getMetrics() {
        CacheMetrics cache = redisService.getCacheMetrics();

        long queueDepth = rabbitMQService.getQueueDepth(RabbitConfig.CLICK_EVENTS_QUEUE);
        long dlqDepth = rabbitMQService.getQueueDepth(RabbitConfig.CLICK_EVENTS_DLQ);
        long processedEvents = analyticsRepository.countTotalProcessedEvents();

        QueueMetrics queue = new QueueMetrics(queueDepth, dlqDepth, processedEvents);

        List<WorkerStatus> workers = redisService.getWorkerFleetStatus();
        DbPoolMetrics dbPool = databaseRepository.getDbPoolMetrics();

        return new SystemMetricsResponse(
            cache,
            queue,
            workers,
            dbPool.poolSize(),
            dbPool.checkedOut()
        );
    }
}
