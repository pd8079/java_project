package com.cyberopsarena.service;

import com.cyberopsarena.security.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

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

    public Map<String, Object> currentUser(String username) {
        return jdbc.queryForMap("SELECT id,username,email,role,enabled,created_at FROM users WHERE username=?", username);
    }
}
