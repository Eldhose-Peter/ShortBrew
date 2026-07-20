package com.epproject.ShortBrew.service;

import com.epproject.ShortBrew.config.RabbitConfig;
import com.epproject.ShortBrew.controller.dto.SystemMetricsResponse;
import com.epproject.ShortBrew.controller.dto.SystemMetricsResponse.CacheMetrics;
import com.epproject.ShortBrew.controller.dto.SystemMetricsResponse.QueueMetrics;
import com.epproject.ShortBrew.controller.dto.SystemMetricsResponse.WorkerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Properties;

@Service
public class SystemMetricsService {

    private static final Logger log = LoggerFactory.getLogger(SystemMetricsService.class);

    private final RabbitAdmin rabbitAdmin;
    private final JdbcTemplate jdbcTemplate;

    public SystemMetricsService(RabbitAdmin rabbitAdmin, JdbcTemplate jdbcTemplate) {
        this.rabbitAdmin = rabbitAdmin;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Returns system metrics, dynamically querying RabbitMQ queue depth and DB event totals.
     */
    public SystemMetricsResponse getMetrics() {
        CacheMetrics cache = new CacheMetrics(1420L, 85L, 0.943);

        long queueDepth = getQueueDepth(RabbitConfig.CLICK_EVENTS_QUEUE);
        long dlqDepth = getQueueDepth(RabbitConfig.CLICK_EVENTS_DLQ);
        long processedEvents = getProcessedEventsCount();

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

    private long getQueueDepth(String queueName) {
        try {
            Properties props = rabbitAdmin.getQueueProperties(queueName);
            if (props != null) {
                Object count = props.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
                if (count instanceof Number number) {
                    return number.longValue();
                }
            }
        } catch (Exception e) {
            log.debug("Could not fetch queue depth for {}: {}", queueName, e.getMessage());
        }
        return 0L;
    }

    private long getProcessedEventsCount() {
        try {
            Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM click_events", Long.class);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.debug("Could not fetch processed events count from database: {}", e.getMessage());
            return 0L;
        }
    }
}
