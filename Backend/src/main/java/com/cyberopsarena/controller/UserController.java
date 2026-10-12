package com.cyberopsarena.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import com.cyberopsarena.service.UserService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/v1/users") @PreAuthorize("hasRole('ADMIN')")
public class UserController {
 private final UserService users;
 public UserController(UserService users){this.users=users;}
 public record CreateUser(@NotBlank @Size(max=80) String username,@NotBlank @Email @Size(max=254) String email,@NotBlank @Size(min=14,max=100) String password,@NotBlank String role){}
 public record PatchUser(@NotBlank @Size(max=80) String username,@NotBlank @Email @Size(max=254) String email,@NotBlank String role){}
 public record EnabledRequest(boolean enabled){}
 @GetMapping public Object list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size){return users.list(page,size);}
 @GetMapping("/{id}") public Map<String,Object> get(@PathVariable long id){return users.get(id);}
 @PostMapping public Map<String,Object> create(@Valid @RequestBody CreateUser req){return users.create(req.username(),req.email(),req.password(),req.role());}
 @PatchMapping("/{id}") public Map<String,Object> update(@PathVariable long id,@Valid @RequestBody PatchUser req){return users.update(id,req.username(),req.email(),req.role());}
 @PatchMapping("/{id}/enabled") public Map<String,Object> enabled(@PathVariable long id,@Valid @RequestBody EnabledRequest req){return users.setEnabled(id,req.enabled());}
 @DeleteMapping("/{id}") public void delete(@PathVariable long id){users.delete(id);}
}
