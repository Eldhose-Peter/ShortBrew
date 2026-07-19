package com.epproject.ShortBrew.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record SystemMetricsResponse(
    @JsonProperty("cache") CacheMetrics cache,
    @JsonProperty("queue") QueueMetrics queue,
    @JsonProperty("workers") List<WorkerStatus> workers,
    @JsonProperty("db_pool_size") int dbPoolSize,
    @JsonProperty("db_pool_checked_out") int dbPoolCheckedOut
) {
    public record CacheMetrics(
        @JsonProperty("hits") long hits,
        @JsonProperty("misses") long misses,
        @JsonProperty("hit_rate") double hitRate
    ) {}

    public record QueueMetrics(
        @JsonProperty("queue_depth") long queueDepth,
        @JsonProperty("dlq_depth") long dlqDepth,
        @JsonProperty("processed_events") long processedEvents
    ) {}

    public record WorkerStatus(
        @JsonProperty("worker_id") String workerId,
        @JsonProperty("last_heartbeat_seconds_ago") double lastHeartbeatSecondsAgo,
        @JsonProperty("alive") boolean alive
    ) {}
}
