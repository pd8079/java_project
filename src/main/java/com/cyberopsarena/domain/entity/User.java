package com.cyberopsarena.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** Persistence mapping for the account table. Authentication secrets never leave the service layer. */
@Entity @Table(name="users")
public class User {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=80) private String username;
 @Column(nullable=false,unique=true,length=254) private String email;
 @Column(name="password_hash",nullable=false,length=100) private String passwordHash;
 @Column(nullable=false,length=20) private String role;
 @Column(nullable=false) private boolean enabled=true;
 @Column(name="created_at",nullable=false,insertable=false,updatable=false) private Instant createdAt;
 @Column(name="updated_at",nullable=false,insertable=false,updatable=false) private Instant updatedAt;
 protected User() {}
 public Long getId(){return id;} public String getUsername(){return username;} public String getEmail(){return email;} public String getRole(){return role;} public boolean isEnabled(){return enabled;}
}
