package io.github.sungjh.aiusagebillinggateway.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.sungjh.aiusagebillinggateway.common.Hashing;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class JwtService {

    private final ObjectMapper objectMapper;
    private final String secret;
    private final long ttlSeconds;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.ttl-seconds}") long ttlSeconds) {
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.ttlSeconds = ttlSeconds;
    }

    public String createToken(UUID userId, String email) {
        try {
            String header = encode(objectMapper.writeValueAsString(Map.of(
                    "alg", "HS256",
                    "typ", "JWT")));
            String payload = encode(objectMapper.writeValueAsString(Map.of(
                    "sub", userId.toString(),
                    "email", email,
                    "exp", Instant.now().plusSeconds(ttlSeconds).getEpochSecond())));
            String unsigned = header + "." + payload;
            return unsigned + "." + sign(unsigned);
        } catch (Exception exception) {
            throw new IllegalStateException("JWT creation failed", exception);
        }
    }

    public AuthenticatedUser parse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw unauthorized();
            }
            String unsigned = parts[0] + "." + parts[1];
            if (!Hashing.constantTimeEquals(sign(unsigned), parts[2])) {
                throw unauthorized();
            }
            JsonNode payload = objectMapper.readTree(decode(parts[1]));
            if (payload.get("exp").asLong() < Instant.now().getEpochSecond()) {
                throw unauthorized();
            }
            return new AuthenticatedUser(
                    UUID.fromString(payload.get("sub").asText()),
                    payload.get("email").asText());
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw unauthorized();
        }
    }

    private String sign(String unsigned) {
        byte[] signature = java.util.HexFormat.of()
                .parseHex(Hashing.hmacSha256Hex(secret, unsigned));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
    }

    private String encode(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid JWT");
    }
}
