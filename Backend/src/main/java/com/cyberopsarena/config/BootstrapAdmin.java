package com.cyberopsarena.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class BootstrapAdmin {
 @Bean ApplicationRunner adminBootstrapRunner(JdbcTemplate jdbc, PasswordEncoder encoder,
    @Value("${BOOTSTRAP_ADMIN_USERNAME:}") String username,
    @Value("${BOOTSTRAP_ADMIN_EMAIL:}") String email,
    @Value("${BOOTSTRAP_ADMIN_PASSWORD:}") String password) {
   return args -> { if(username.isBlank() && email.isBlank() && password.isBlank()) return;
     if(username.isBlank() || email.isBlank() || password.length()<14) throw new IllegalStateException("Set all bootstrap admin values; password must be at least 14 characters");
     if(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE username=?",Integer.class,username)==0)
       jdbc.update("INSERT INTO users(username,email,password_hash,role,enabled) VALUES(?,?,?,'ADMIN',TRUE)",username,email,encoder.encode(password));
   };
 }
}
