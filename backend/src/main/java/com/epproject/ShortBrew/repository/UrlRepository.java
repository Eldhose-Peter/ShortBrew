package com.epproject.ShortBrew.repository;

import com.epproject.ShortBrew.model.Url;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UrlRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public UrlRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Url> urlRowMapper = (rs, rowNum) -> {
        Timestamp createdAtTimestamp = rs.getTimestamp("created_at");
        Timestamp expiresAtTimestamp = rs.getTimestamp("expires_at");
        return new Url(
            rs.getLong("id"),
            rs.getString("short_code"),
            rs.getString("custom_alias"),
            rs.getString("target_url"),
            rs.getString("title"),
            rs.getObject("owner_id", UUID.class),
            createdAtTimestamp != null ? createdAtTimestamp.toInstant() : null,
            expiresAtTimestamp != null ? expiresAtTimestamp.toInstant() : null,
            rs.getBoolean("is_active"),
            rs.getLong("total_clicks")
        );
    };

    public Optional<Url> findById(Long id) {
        String sql = "SELECT id, short_code, custom_alias, target_url, title, owner_id, created_at, expires_at, is_active, total_clicks " +
                     "FROM urls WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        try {
            Url url = jdbc.queryForObject(sql, params, urlRowMapper);
            return Optional.ofNullable(url);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<Url> findByCustomAlias(String customAlias) {
        String sql = "SELECT id, short_code, custom_alias, target_url, title, owner_id, created_at, expires_at, is_active, total_clicks " +
                     "FROM urls WHERE custom_alias = :custom_alias";
        MapSqlParameterSource params = new MapSqlParameterSource("custom_alias", customAlias);
        try {
            Url url = jdbc.queryForObject(sql, params, urlRowMapper);
            return Optional.ofNullable(url);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<Url> findByShortCode(String shortCode) {
        String sql = "SELECT id, short_code, custom_alias, target_url, title, owner_id, created_at, expires_at, is_active, total_clicks " +
                     "FROM urls WHERE short_code = :short_code";
        MapSqlParameterSource params = new MapSqlParameterSource("short_code", shortCode);
        try {
            Url url = jdbc.queryForObject(sql, params, urlRowMapper);
            return Optional.ofNullable(url);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Url save(Url url) {
        String sql = "INSERT INTO urls (short_code, custom_alias, target_url, title, owner_id, expires_at, is_active) " +
                     "VALUES (:short_code, :custom_alias, :target_url, :title, :owner_id, :expires_at, :is_active) " +
                     "RETURNING id, short_code, custom_alias, target_url, title, owner_id, created_at, expires_at, is_active, total_clicks";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("short_code", url.shortCode())
            .addValue("custom_alias", url.customAlias())
            .addValue("target_url", url.targetUrl())
            .addValue("title", url.title())
            .addValue("owner_id", url.ownerId())
            .addValue("expires_at", url.expiresAt() != null ? Timestamp.from(url.expiresAt()) : null)
            .addValue("is_active", url.isActive());
        return jdbc.queryForObject(sql, params, urlRowMapper);
    }

    public Url update(Url url) {
        String sql = "UPDATE urls SET " +
                     "target_url = :target_url, " +
                     "expires_at = :expires_at, " +
                     "is_active = :is_active, " +
                     "title = :title " +
                     "WHERE id = :id " +
                     "RETURNING id, short_code, custom_alias, target_url, title, owner_id, created_at, expires_at, is_active, total_clicks";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("id", url.id())
            .addValue("target_url", url.targetUrl())
            .addValue("expires_at", url.expiresAt() != null ? Timestamp.from(url.expiresAt()) : null)
            .addValue("is_active", url.isActive())
            .addValue("title", url.title());
        return jdbc.queryForObject(sql, params, urlRowMapper);
    }

    public Url updateShortCode(Long id, String shortCode) {
        String sql = "UPDATE urls SET " +
                     "short_code = :short_code " +
                     "WHERE id = :id " +
                     "RETURNING id, short_code, custom_alias, target_url, title, owner_id, created_at, expires_at, is_active, total_clicks";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("short_code", shortCode);
        return jdbc.queryForObject(sql, params, urlRowMapper);
    }

    public void delete(Long id) {
        String sql = "DELETE FROM urls WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        jdbc.update(sql, params);
    }

    public static record UrlSearchResult(List<Url> items, long total) {}

    public UrlSearchResult search(UUID ownerId, int page, int pageSize, String search, Boolean isActive) {
        StringBuilder sql = new StringBuilder("SELECT id, short_code, custom_alias, target_url, title, owner_id, created_at, expires_at, is_active, total_clicks FROM urls WHERE owner_id = :owner_id");
        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM urls WHERE owner_id = :owner_id");
        MapSqlParameterSource params = new MapSqlParameterSource("owner_id", ownerId);

        if (search != null && !search.isBlank()) {
            String searchCond = " AND (target_url ILIKE :search OR short_code ILIKE :search OR custom_alias ILIKE :search OR title ILIKE :search)";
            sql.append(searchCond);
            countSql.append(searchCond);
            params.addValue("search", "%" + search.trim() + "%");
        }

        if (isActive != null) {
            String activeCond = " AND is_active = :is_active";
            sql.append(activeCond);
            countSql.append(activeCond);
            params.addValue("is_active", isActive);
        }

        Long total = jdbc.queryForObject(countSql.toString(), params, Long.class);
        long totalCount = total != null ? total : 0L;

        sql.append(" ORDER BY created_at DESC LIMIT :limit OFFSET :offset");
        params.addValue("limit", pageSize);
        params.addValue("offset", (page - 1) * pageSize);

        List<Url> items = jdbc.query(sql.toString(), params, urlRowMapper);

        return new UrlSearchResult(items, totalCount);
    }
}
