package com.cyberopsarena.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

@RestController @RequestMapping("/api/v1/users") @PreAuthorize("hasRole('ADMIN')")
public class UserController {
 private final JdbcTemplate jdbc;private final PasswordEncoder encoder;
 public UserController(JdbcTemplate jdbc,PasswordEncoder encoder){this.jdbc=jdbc;this.encoder=encoder;}
 public record CreateUser(@NotBlank @Size(max=80) String username,@NotBlank @Email @Size(max=254) String email,@NotBlank @Size(min=14,max=100) String password,@NotBlank String role){}
 public record PatchUser(@NotBlank @Size(max=80) String username,@NotBlank @Email @Size(max=254) String email,@NotBlank String role){}
 public record EnabledRequest(boolean enabled){}
 @GetMapping public Object list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size){if(page<0||size<1||size>100)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Invalid pagination");return jdbc.queryForList("SELECT id,username,email,role,enabled,created_at,updated_at FROM users ORDER BY id LIMIT ? OFFSET ?",size,page*size);}
 @GetMapping("/{id}") public Object get(@PathVariable long id){return jdbc.queryForMap("SELECT id,username,email,role,enabled,created_at,updated_at FROM users WHERE id=?",id);}
 @PostMapping @Transactional public Object create(@Valid @RequestBody CreateUser req){checkRole(req.role());jdbc.update("INSERT INTO users(username,email,password_hash,role,enabled) VALUES(?,?,?,?,TRUE)",req.username(),req.email(),encoder.encode(req.password()),req.role());return get(jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class));}
 @PatchMapping("/{id}") public Object update(@PathVariable long id,@Valid @RequestBody PatchUser req){get(id);checkRole(req.role());jdbc.update("UPDATE users SET username=?,email=?,role=? WHERE id=?",req.username(),req.email(),req.role(),id);return get(id);}
 @PatchMapping("/{id}/enabled") public Object enabled(@PathVariable long id,@Valid @RequestBody EnabledRequest req){get(id);jdbc.update("UPDATE users SET enabled=? WHERE id=?",req.enabled(),id);return get(id);}
 private void checkRole(String role){if(!Set.of("ADMIN","ANALYST","DEFENDER").contains(role))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Unsupported user role");}
}
