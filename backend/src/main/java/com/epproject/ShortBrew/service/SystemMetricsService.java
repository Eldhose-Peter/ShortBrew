package com.epproject.ShortBrew.service;

import com.epproject.ShortBrew.controller.dto.SystemMetricsResponse;
import com.epproject.ShortBrew.controller.dto.SystemMetricsResponse.CacheMetrics;
import com.epproject.ShortBrew.controller.dto.SystemMetricsResponse.QueueMetrics;
import com.epproject.ShortBrew.controller.dto.SystemMetricsResponse.WorkerStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SystemMetricsService {

    /**
     * Returns mock system metrics matching the Architecture Dashboard expectations.
     */
    public SystemMetricsResponse getMetrics() {
        CacheMetrics cache = new CacheMetrics(1420L, 85L, 0.943);
        QueueMetrics queue = new QueueMetrics(0L, 0L, 15085L);

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
