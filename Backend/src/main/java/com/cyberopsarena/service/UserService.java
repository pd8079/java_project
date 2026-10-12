package com.cyberopsarena.service;

import com.cyberopsarena.repository.UserJdbcRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class UserService {
    private static final Set<String> ROLES = Set.of("ADMIN", "ANALYST", "DEFENDER");
    private final UserJdbcRepository users;
    private final PasswordEncoder passwords;

    public UserService(UserJdbcRepository users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw badRequest("Invalid pagination");
        return users.findPage(page, size);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(long id) {
        return users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Transactional
    public Map<String, Object> create(String username, String email, String rawPassword, String role) {
        validateRole(role);
        long id = users.create(username, email, passwords.encode(rawPassword), role);
        return get(id);
    }

    @Transactional
    public Map<String, Object> update(long id, String username, String email, String role) {
        validateRole(role);
        get(id);
        users.update(id, username, email, role);
        return get(id);
    }

    @Transactional
    public Map<String, Object> setEnabled(long id, boolean enabled) {
        get(id);
        users.updateEnabled(id, enabled);
        return get(id);
    }

    @Transactional
    public void delete(long id) {
        if (!users.deleteById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
    }

    private void validateRole(String role) {
        if (!ROLES.contains(role)) throw badRequest("Unsupported user role");
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
