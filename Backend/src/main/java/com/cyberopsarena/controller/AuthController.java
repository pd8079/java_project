package com.cyberopsarena.controller;

import com.cyberopsarena.service.AuthenticationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/v1/auth")
public class AuthController {
 private final AuthenticationService authentication;
 public AuthController(AuthenticationService authentication){this.authentication=authentication;}
 public record LoginRequest(@NotBlank String username,@NotBlank String password){}
 public record RegisterRequest(@NotBlank @Size(max=80) String username,
   @NotBlank @Email @Size(max=254) String email,
   @NotBlank @Size(min=14,max=100) String password,
   @NotBlank @Pattern(regexp="ANALYST|DEFENDER") String role){}
 @PostMapping("/login") public Map<String,Object> login(@Valid @RequestBody LoginRequest request){
   return authentication.login(request.username(),request.password());
 }
 @PostMapping("/register") public Map<String,Object> register(@Valid @RequestBody RegisterRequest request){
   return authentication.register(request.username(),request.email(),request.password(),request.role());
 }
 @GetMapping("/me") public Map<String,Object> me(Authentication auth){return authentication.currentUser(auth.getName());}
}
