package com.epproject.ShortBrew.repository;

import com.epproject.ShortBrew.model.User;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public UserRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
        Timestamp createdAtTimestamp = rs.getTimestamp("created_at");
        return new User(
            rs.getObject("id", UUID.class),
            rs.getString("email"),
            rs.getString("hashed_password"),
            rs.getString("full_name"),
            rs.getBoolean("is_active"),
            createdAtTimestamp != null ? createdAtTimestamp.toInstant() : null
        );
    };

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, email, hashed_password, full_name, is_active, created_at FROM users WHERE email = :email";
        MapSqlParameterSource params = new MapSqlParameterSource("email", email);
        try {
            User user = jdbc.queryForObject(sql, params, userRowMapper);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<User> findById(UUID id) {
        String sql = "SELECT id, email, hashed_password, full_name, is_active, created_at FROM users WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        try {
            User user = jdbc.queryForObject(sql, params, userRowMapper);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public User save(String email, String hashedPassword, String fullName) {
        String sql = "INSERT INTO users (email, hashed_password, full_name) " +
                     "VALUES (:email, :hashed_password, :full_name) " +
                     "RETURNING id, email, hashed_password, full_name, is_active, created_at";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("email", email)
            .addValue("hashed_password", hashedPassword)
            .addValue("full_name", fullName);
        return jdbc.queryForObject(sql, params, userRowMapper);
    }
}
