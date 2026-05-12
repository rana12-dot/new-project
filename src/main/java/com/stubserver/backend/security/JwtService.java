package com.stubserver.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stubserver.backend.config.AppProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    private SecretKey accessKey() {
        return Keys.hmacShaKeyFor(appProperties.getJwt().getAccessSecret().getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey refreshKey() {
        return Keys.hmacShaKeyFor(appProperties.getJwt().getRefreshSecret().getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey resetKey() {
        return Keys.hmacShaKeyFor(appProperties.getJwt().getResetSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String username, String userRole) {
        long now = System.currentTimeMillis();
        long exp = now + appProperties.getJwt().getAccessExpiresSeconds() * 1000L;
        return Jwts.builder()
                .claims(Map.of("username", username, "userRole", userRole))
                .subject(username)
                .issuedAt(new Date(now))
                .expiration(new Date(exp))
                .signWith(accessKey())
                .compact();
    }

    public record RefreshTokenResult(String token, String jti) {}

    public RefreshTokenResult generateRefreshToken(String username) {
        String jti = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        long exp = now + appProperties.getJwt().getRefreshExpiresSeconds() * 1000L;
        String token = Jwts.builder()
                .claims(Map.of("username", username, "jti", jti))
                .subject(username)
                .id(jti)
                .issuedAt(new Date(now))
                .expiration(new Date(exp))
                .signWith(refreshKey())
                .compact();
        return new RefreshTokenResult(token, jti);
    }

    public String generateResetToken(String username) {
        long now = System.currentTimeMillis();
        long exp = now + appProperties.getJwt().getResetExpiresSeconds() * 1000L;
        return Jwts.builder()
                .claims(Map.of("username", username, "purpose", "password_reset"))
                .subject(username)
                .issuedAt(new Date(now))
                .expiration(new Date(exp))
                .signWith(resetKey())
                .compact();
    }

    public Claims verifyAccessToken(String token) {
        return Jwts.parser().verifyWith(accessKey()).build().parseSignedClaims(token).getPayload();
    }

    public Claims verifyRefreshToken(String token) {
        return Jwts.parser().verifyWith(refreshKey()).build().parseSignedClaims(token).getPayload();
    }

    public Claims verifyResetToken(String token) {
        return Jwts.parser().verifyWith(resetKey()).build().parseSignedClaims(token).getPayload();
    }

    public Claims decodeWithoutVerify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;
            byte[] decoded = Base64.getUrlDecoder().decode(padBase64(parts[1]));
            String json = new String(decoded, StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            return Jwts.claims().add(map).build();
        } catch (Exception e) {
            return null;
        }
    }

    private static String padBase64(String s) {
        int pad = s.length() % 4;
        if (pad == 2) return s + "==";
        if (pad == 3) return s + "=";
        return s;
    }

    public long getAccessExpiresSeconds() {
        return appProperties.getJwt().getAccessExpiresSeconds();
    }
}
