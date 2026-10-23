package com.cyberopsarena.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminBootstrap {
    @Bean
    ApplicationRunner bootstrapAdmin(
            JdbcTemplate jdbc,
            PasswordEncoder passwords,
            @Value("${BOOTSTRAP_ADMIN_USERNAME:}") String username,
            @Value("${BOOTSTRAP_ADMIN_PASSWORD:}") String password,
            @Value("${BOOTSTRAP_ADMIN_EMAIL:}") String configuredEmail) {
        return (ApplicationArguments args) -> {
            if (username.isBlank() && password.isBlank()) return;
            if (username.isBlank() || password.isBlank()) {
                throw new IllegalStateException("Both bootstrap admin username and password must be configured");
            }

            var existing = jdbc.query("SELECT role FROM users WHERE username=?",
                    (rs, row) -> rs.getString(1), username.trim());
            if (!existing.isEmpty()) {
                if (!"ADMIN".equals(existing.getFirst())) {
                    throw new IllegalStateException("Bootstrap admin username already belongs to a non-admin account");
                }
                return;
            }

            String email = configuredEmail.isBlank()
                    ? username.trim() + "@demo.invalid"
                    : configuredEmail.trim();
            jdbc.update("INSERT INTO users(username,email,password_hash,role,enabled) VALUES(?,?,?,'ADMIN',TRUE)",
                    username.trim(), email, passwords.encode(password));
        };
    }
}
