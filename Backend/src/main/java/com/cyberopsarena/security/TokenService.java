package com.cyberopsarena.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Component
public class TokenService {
    private final byte[] secret;
    private final long lifetime;
    private final ObjectMapper mapper = new ObjectMapper();
    public TokenService(@Value("${app.jwt-secret:}") String secret, @Value("${app.jwt-expiration-seconds:1800}") long lifetime) {
        if (secret == null || secret.length() < 32) throw new IllegalStateException("JWT_SECRET must contain at least 32 characters");
        this.secret = secret.getBytes(StandardCharsets.UTF_8); this.lifetime = lifetime;
    }
    public String issue(String username) {
        try {
            String header = enc("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
            String claims = enc(mapper.writeValueAsBytes(Map.of("sub", username, "iat", Instant.now().getEpochSecond(), "exp", Instant.now().plusSeconds(lifetime).getEpochSecond())));
            String data = header + "." + claims;
            return data + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(sign(data));
        } catch (Exception e) { throw new IllegalStateException("Token generation failed", e); }
    }
    public String subject(String token) {
        try {
            String[] parts = token.split("\\."); if (parts.length != 3) return null;
            String data = parts[0] + "." + parts[1];
            if (!java.security.MessageDigest.isEqual(sign(data), Base64.getUrlDecoder().decode(parts[2]))) return null;
            Map<?,?> claims = mapper.readValue(Base64.getUrlDecoder().decode(parts[1]), Map.class);
            if (!(claims.get("exp") instanceof Number exp) || exp.longValue() <= Instant.now().getEpochSecond()) return null;
            return (String) claims.get("sub");
        } catch (Exception e) { return null; }
    }
    private byte[] sign(String value) throws Exception { Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(secret,"HmacSHA256")); return mac.doFinal(value.getBytes(StandardCharsets.UTF_8)); }
    private String enc(String value) { return enc(value.getBytes(StandardCharsets.UTF_8)); }
    private String enc(byte[] value) { return Base64.getUrlEncoder().withoutPadding().encodeToString(value); }
}
