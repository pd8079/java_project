package com.cyberopsarena.config;

import com.cyberopsarena.security.JwtFilter;
import com.cyberopsarena.security.TokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.beans.factory.annotation.Value;
import java.util.Arrays;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder(){ return new BCryptPasswordEncoder(12); }
    @Bean SecurityFilterChain security(HttpSecurity http, TokenService tokens, JdbcTemplate jdbc, @Value("${app.cors-origins}") String origins) throws Exception {
        http.csrf(c->c.disable()).cors(c->c.configurationSource(req->{ var cfg=new CorsConfiguration(); cfg.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).toList()); cfg.setAllowedMethods(java.util.List.of("GET","POST","PATCH","OPTIONS")); cfg.setAllowedHeaders(java.util.List.of("Authorization","Content-Type")); return cfg; })));
        http.sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.authorizeHttpRequests(a->a.requestMatchers("/api/v1/auth/login","/actuator/health/**","/swagger-ui/**","/v3/api-docs/**").permitAll().anyRequest().authenticated());
        http.addFilterBefore(new JwtFilter(tokens,jdbc), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
