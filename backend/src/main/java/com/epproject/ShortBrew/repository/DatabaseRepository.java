package com.epproject.ShortBrew.repository;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

@Repository
public class DatabaseRepository {

    private static final Logger log = LoggerFactory.getLogger(DatabaseRepository.class);

    private final DataSource dataSource;

    public DatabaseRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public record DbPoolMetrics(int poolSize, int checkedOut) {}

    /**
     * Inspects HikariCP pool metrics (maximum pool size and active checked out connections).
     */
    public DbPoolMetrics getDbPoolMetrics() {
        try {
            if (dataSource instanceof HikariDataSource hikari) {
                int poolSize = hikari.getMaximumPoolSize();
                int checkedOut = (hikari.getHikariPoolMXBean() != null)
                        ? hikari.getHikariPoolMXBean().getActiveConnections()
                        : 0;
                return new DbPoolMetrics(poolSize, checkedOut);
            }
        } catch (Exception e) {
            log.debug("Could not fetch HikariCP connection pool metrics: {}", e.getMessage());
        }
        return new DbPoolMetrics(10, 0);
    }
}
