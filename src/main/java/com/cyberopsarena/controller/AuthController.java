package com.cyberopsarena.controller;

import com.cyberopsarena.security.TokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController @RequestMapping("/api/v1/auth")
public class AuthController {
 private final JdbcTemplate jdbc; private final PasswordEncoder passwords; private final TokenService tokens;
 public AuthController(JdbcTemplate jdbc,PasswordEncoder passwords,TokenService tokens){this.jdbc=jdbc;this.passwords=passwords;this.tokens=tokens;}
 public record LoginRequest(@NotBlank String username,@NotBlank String password){}
 @PostMapping("/login") public Map<String,Object> login(@Valid @RequestBody LoginRequest request){
   var rows=jdbc.query("SELECT password_hash,role,enabled FROM users WHERE username=?",(rs,n)->new Object[]{rs.getString(1),rs.getString(2),rs.getBoolean(3)},request.username());
   if(rows.isEmpty() || !(Boolean)rows.getFirst()[2] || !passwords.matches(request.password(),(String)rows.getFirst()[0])) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid credentials");
   return Map.of("accessToken",tokens.issue(request.username()),"tokenType","Bearer","role",rows.getFirst()[1]);
 }
 @GetMapping("/me") public Map<String,Object> me(Authentication auth){return jdbc.queryForMap("SELECT id,username,email,role,enabled,created_at FROM users WHERE username=?",auth.getName());}
}
