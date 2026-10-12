package com.cyberopsarena.controller;

import com.cyberopsarena.service.AuthenticationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/v1/auth")
public class AuthController {
 private final AuthenticationService authentication;
 public AuthController(AuthenticationService authentication){this.authentication=authentication;}
 public record LoginRequest(@NotBlank String username,@NotBlank String password){}
 @PostMapping("/login") public Map<String,Object> login(@Valid @RequestBody LoginRequest request){
   return authentication.login(request.username(),request.password());
 }
 @GetMapping("/me") public Map<String,Object> me(Authentication auth){return authentication.currentUser(auth.getName());}
}
