package com.cyberopsarena.repository;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Explicit JDBC persistence for administrative user CRUD. */
@Repository
public class UserJdbcRepository {
    private final DataSource dataSource;

    public UserJdbcRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public long create(String username, String email, String passwordHash, String role) {
        return withConnection(connection -> {
            String sql = "INSERT INTO users(username,email,password_hash,role,enabled) VALUES(?,?,?,?,TRUE)";
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, username);
                statement.setString(2, email);
                statement.setString(3, passwordHash);
                statement.setString(4, role);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("User insert returned no generated id");
                    return keys.getLong(1);
                }
            }
        });
    }

    public Optional<Map<String, Object>> findById(long id) {
        return withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id,username,email,role,enabled,created_at,updated_at FROM users WHERE id=?")) {
                statement.setLong(1, id);
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next() ? Optional.of(toUser(rows)) : Optional.empty();
                }
            }
        });
    }

    public List<Map<String, Object>> findPage(int page, int size) {
        int offset = Math.multiplyExact(page, size);
        return withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id,username,email,role,enabled,created_at,updated_at FROM users ORDER BY id LIMIT ? OFFSET ?")) {
                statement.setInt(1, size);
                statement.setInt(2, offset);
                try (ResultSet rows = statement.executeQuery()) {
                    List<Map<String, Object>> users = new ArrayList<>();
                    while (rows.next()) users.add(toUser(rows));
                    return users;
                }
            }
        });
    }

    public boolean update(long id, String username, String email, String role) {
        return withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE users SET username=?,email=?,role=? WHERE id=?")) {
                statement.setString(1, username);
                statement.setString(2, email);
                statement.setString(3, role);
                statement.setLong(4, id);
                return statement.executeUpdate() == 1;
            }
        });
    }

    public boolean updateEnabled(long id, boolean enabled) {
        return withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE users SET enabled=? WHERE id=?")) {
                statement.setBoolean(1, enabled);
                statement.setLong(2, id);
                return statement.executeUpdate() == 1;
            }
        });
    }

    public boolean deleteById(long id) {
        return withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM users WHERE id=?")) {
                statement.setLong(1, id);
                return statement.executeUpdate() == 1;
            }
        });
    }

    private Map<String, Object> toUser(ResultSet row) throws SQLException {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", row.getLong("id"));
        user.put("username", row.getString("username"));
        user.put("email", row.getString("email"));
        user.put("role", row.getString("role"));
        user.put("enabled", row.getBoolean("enabled"));
        user.put("created_at", row.getTimestamp("created_at"));
        user.put("updated_at", row.getTimestamp("updated_at"));
        return user;
    }

    private <T> T withConnection(SqlWork<T> work) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            return work.execute(connection);
        } catch (SQLException exception) {
            throw new DataAccessResourceFailureException("User JDBC operation failed", exception);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    @FunctionalInterface
    private interface SqlWork<T> {
        T execute(Connection connection) throws SQLException;
    }
}
