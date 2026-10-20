package com.cyberopsarena.service;

import com.cyberopsarena.security.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Locale;
import java.util.Set;

@Service
public class AuthenticationService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwords;
    private final TokenService tokens;

    public AuthenticationService(JdbcTemplate jdbc, PasswordEncoder passwords, TokenService tokens) {
        this.jdbc = jdbc;
        this.passwords = passwords;
        this.tokens = tokens;
    }

    public Map<String, Object> login(String username, String password) {
        var rows = jdbc.query("SELECT password_hash,role,enabled FROM users WHERE username=?",
                (rs, row) -> new Object[]{rs.getString(1), rs.getString(2), rs.getBoolean(3)}, username);
        if (rows.isEmpty() || !(Boolean) rows.getFirst()[2]
                || !passwords.matches(password, (String) rows.getFirst()[0])) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return Map.of("accessToken", tokens.issue(username), "tokenType", "Bearer", "role", rows.getFirst()[1]);
    }

    @Transactional
    public Map<String, Object> register(String username, String email, String password, String role) {
        if (!Set.of("ANALYST", "DEFENDER").contains(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New accounts must use the Analyst or Defender role");
        }
        String cleanUsername = username.trim();
        String cleanEmail = email.trim().toLowerCase(Locale.ROOT);
        Integer existing = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username=? OR email=?",
                Integer.class, cleanUsername, cleanEmail);
        if (existing != null && existing > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username or email is already registered");
        }
        try {
            jdbc.update("INSERT INTO users(username,email,password_hash,role,enabled) VALUES(?,?,?,?,TRUE)",
                    cleanUsername, cleanEmail, passwords.encode(password), role);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username or email is already registered");
        }
        return Map.of("accessToken", tokens.issue(cleanUsername), "tokenType", "Bearer", "role", role);
    }

    public Map<String, Object> currentUser(String username) {
        return jdbc.queryForMap("SELECT id,username,email,role,enabled,created_at FROM users WHERE username=?", username);
    }
}
