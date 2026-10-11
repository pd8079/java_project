package com.cyberopsarena.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

public class JwtFilter extends OncePerRequestFilter {
    private final TokenService tokens; private final JdbcTemplate jdbc;
    public JwtFilter(TokenService tokens, JdbcTemplate jdbc) { this.tokens=tokens; this.jdbc=jdbc; }
    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        String auth=req.getHeader("Authorization");
        if(auth!=null && auth.startsWith("Bearer ")) {
            String username=tokens.subject(auth.substring(7));
            if(username!=null) {
                var users=jdbc.query("SELECT role FROM users WHERE username=? AND enabled=TRUE", (rs,n)->rs.getString(1), username);
                if(!users.isEmpty()) SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(username,null,java.util.List.of(new SimpleGrantedAuthority("ROLE_"+users.getFirst()))));
            }
        }
        chain.doFilter(req,res);
    }
}
