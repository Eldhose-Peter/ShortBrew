package com.epproject.ShortBrew.repository;

import com.epproject.ShortBrew.controller.dto.CountryStat;
import com.epproject.ShortBrew.controller.dto.DailyClickPoint;
import com.epproject.ShortBrew.controller.dto.ReferrerStat;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public class AnalyticsRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnalyticsRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static record DailyStatRow(
        LocalDate statDate,
        long clickCount,
        String referrerBreakdownJson,
        String countryBreakdownJson
    ) {}

    private List<DailyStatRow> fetchDailyStats(Long urlId, int days) {
        String sql = "SELECT stat_date, click_count, referrer_breakdown, country_breakdown " +
                     "FROM url_daily_stats " +
                     "WHERE url_id = :url_id AND stat_date >= CURRENT_DATE - CAST(:days AS INT)";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("url_id", urlId)
            .addValue("days", days);
        return jdbc.query(sql, params, (rs, rowNum) -> new DailyStatRow(
            rs.getDate("stat_date").toLocalDate(),
            rs.getLong("click_count"),
            rs.getString("referrer_breakdown"),
            rs.getString("country_breakdown")
        ));
    }

    public List<DailyClickPoint> dailyClicksForUrl(Long urlId, int days) {
        List<DailyStatRow> rows = fetchDailyStats(urlId, days);
        return rows.stream()
            .map(row -> new DailyClickPoint(row.statDate(), row.clickCount()))
            .sorted(java.util.Comparator.comparing(DailyClickPoint::statDate))
            .toList();
    }

    public List<ReferrerStat> referrerBreakdown(Long urlId, int days) {
        List<DailyStatRow> rows = fetchDailyStats(urlId, days);
        java.util.Map<String, Long> merged = new java.util.HashMap<>();

        for (DailyStatRow row : rows) {
            String json = row.referrerBreakdownJson();
            if (json != null && !json.isBlank()) {
                try {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> map = objectMapper.readValue(json, java.util.Map.class);
                    for (java.util.Map.Entry<String, Object> entry : map.entrySet()) {
                        String ref = entry.getKey();
                        long count = ((Number) entry.getValue()).longValue();
                        merged.put(ref, merged.getOrDefault(ref, 0L) + count);
                    }
                } catch (Exception e) {
                    // Ignore parsing error for this row
                }
            }
        }

        return merged.entrySet().stream()
            .map(e -> new ReferrerStat(e.getKey(), e.getValue()))
            .sorted(java.util.Comparator.comparing(ReferrerStat::count).reversed())
            .limit(10)
            .toList();
    }

    public List<CountryStat> countryBreakdown(Long urlId, int days) {
        List<DailyStatRow> rows = fetchDailyStats(urlId, days);
        java.util.Map<String, Long> merged = new java.util.HashMap<>();

        for (DailyStatRow row : rows) {
            String json = row.countryBreakdownJson();
            if (json != null && !json.isBlank()) {
                try {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> map = objectMapper.readValue(json, java.util.Map.class);
                    for (java.util.Map.Entry<String, Object> entry : map.entrySet()) {
                        String country = entry.getKey();
                        long count = ((Number) entry.getValue()).longValue();
                        merged.put(country, merged.getOrDefault(country, 0L) + count);
                    }
                } catch (Exception e) {
                    // Ignore parsing error for this row
                }
            }
        }

        return merged.entrySet().stream()
            .map(e -> new CountryStat(e.getKey(), e.getValue()))
            .sorted(java.util.Comparator.comparing(CountryStat::count).reversed())
            .limit(10)
            .toList();
    }

    public long clicksTodayForOwner(UUID ownerId) {
        String sql = "SELECT COALESCE(SUM(uds.click_count), 0) " +
                     "FROM url_daily_stats uds " +
                     "JOIN urls u ON uds.url_id = u.id " +
                     "WHERE u.owner_id = :owner_id AND uds.stat_date = CURRENT_DATE";
        MapSqlParameterSource params = new MapSqlParameterSource("owner_id", ownerId);
        Long total = jdbc.queryForObject(sql, params, Long.class);
        return total != null ? total : 0L;
    }

    public List<DailyClickPoint> dailyClicksForOwner(UUID ownerId, int days) {
        String sql = "SELECT uds.stat_date, SUM(uds.click_count) AS click_count " +
                     "FROM url_daily_stats uds " +
                     "JOIN urls u ON uds.url_id = u.id " +
                     "WHERE u.owner_id = :owner_id AND uds.stat_date >= CURRENT_DATE - CAST(:days AS INT) " +
                     "GROUP BY uds.stat_date " +
                     "ORDER BY uds.stat_date ASC";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("owner_id", ownerId)
            .addValue("days", days);
        return jdbc.query(sql, params, (rs, rowNum) -> new DailyClickPoint(
            rs.getDate("stat_date").toLocalDate(),
            rs.getLong("click_count")
        ));
    }

    public long countTotalProcessedEvents() {
        String sql = "SELECT COUNT(*) FROM click_events";
        Long total = jdbc.queryForObject(sql, new MapSqlParameterSource(), Long.class);
        return total != null ? total : 0L;
    }
}
