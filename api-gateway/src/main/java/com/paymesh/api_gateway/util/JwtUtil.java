package com.paymesh.api_gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
public class JwtUtil {

    private static String SECRET ;

    @Value("${jwt.secret}")
    public void setSecret(String secret) {
        JwtUtil.SECRET = secret;
    }

    private static Key getSigningKey() {
        if (SECRET == null || SECRET.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret is not configured. Set JWT_SECRET environment variable or configure jwt.secret in application.yml");
        }
        byte[] keyBytes = SECRET.getBytes();
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret is too short. Use a key with at least 32 bytes.");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public static Claims validateToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
