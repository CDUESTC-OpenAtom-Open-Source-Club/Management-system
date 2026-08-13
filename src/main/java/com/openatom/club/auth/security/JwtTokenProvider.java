package com.openatom.club.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expireMillis;

    public JwtTokenProvider(
            @Value("${app.auth.jwt-secret}") String secret,
            @Value("${app.auth.jwt-expire-hours:24}") int expireHours) {
        // 使用 HMAC-SHA256，key 至少 32 字节
        byte[] keyBytes = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            // 补齐到 32 字节
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expireMillis = (long) expireHours * 3600 * 1000;
    }

    /** 生成 JWT，payload 只放 userId 和 username */
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expireMillis);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /** 解析 userId，如果 token 无效则抛出异常 */
    public Long getUserId(String token) {
        Claims claims = parseClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    public String getUsername(String token) {
        Claims claims = parseClaims(token);
        return claims.get("username", String.class);
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT 无效: {}", e.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
