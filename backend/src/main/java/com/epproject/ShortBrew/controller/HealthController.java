package com.epproject.ShortBrew.controller;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;

    public HealthController(DataSource dataSource, RedisConnectionFactory redisConnectionFactory) {
        this.dataSource = dataSource;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> healthCheck() {
        boolean dbHealthy = false;
        String dbDetails = "Unknown error";
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(2)) {
                dbHealthy = true;
                dbDetails = "Database is responsive";
            } else {
                dbDetails = "Connection isValid validation failed";
            }
        } catch (Exception e) {
            dbDetails = "Database connection error: " + e.getMessage();
        }

        boolean redisHealthy = false;
        String redisDetails = "Unknown error";
        try (RedisConnection conn = redisConnectionFactory.getConnection()) {
            String ping = conn.ping();
            if ("PONG".equalsIgnoreCase(ping)) {
                redisHealthy = true;
                redisDetails = "Redis is responsive (PONG)";
            } else {
                redisDetails = "Unexpected redis response: " + ping;
            }
        } catch (Exception e) {
            redisDetails = "Redis connection error: " + e.getMessage();
        }

        String overallStatus = (dbHealthy && redisHealthy) ? "UP" : "DOWN";
        HealthResponse response = new HealthResponse(
            overallStatus,
            new ServiceHealth(dbHealthy ? "UP" : "DOWN", dbDetails),
            new ServiceHealth(redisHealthy ? "UP" : "DOWN", redisDetails)
        );

        if ("UP".equals(overallStatus)) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }

    public record HealthResponse(
        String status,
        ServiceHealth database,
        ServiceHealth redis
    ) {}

    public record ServiceHealth(
        String status,
        String details
    ) {}
}
