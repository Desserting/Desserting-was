package com.desserting.backend.global.security;

import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:ZGVzc2VydGluZy1zZWNyZXQta2V5LTIwMjYtZGFuZGl0aG9uLWJhY2tlbmQtand0LXRva2VuLXNlY3VyZQ==}")
    private String secretKeyString;

    @Value("${jwt.access-expiration:3600000}") // 1 hour
    private long accessExpirationMs;

    @Value("${jwt.refresh-expiration:1209600000}") // 14 days
    private long refreshExpirationMs;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKeyString);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Long userId, String email, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessExpirationMs);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role)
                .claim("typ", "access")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpirationMs);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("typ", "refresh")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = getClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    public String getEmailFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("email", String.class);
    }

    public String getTokenType(String token) {
        Claims claims = getClaims(token);
        return claims.get("typ", String.class);
    }

    public boolean isAccessToken(String token) {
        String typ = getTokenType(token);
        // 하위 호환성을 위해 typ가 없으면 access로 간주하되, refresh는 명확히 배제
        return !"refresh".equalsIgnoreCase(typ);
    }

    public boolean isRefreshToken(String token) {
        String typ = getTokenType(token);
        return "refresh".equalsIgnoreCase(typ);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("유효하지 않은 JWT 서명입니다: {}", e.getMessage());
            throw new CustomException(ErrorCode.INVALID_JWT_TOKEN);
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다: {}", e.getMessage());
            throw new CustomException(ErrorCode.EXPIRED_JWT_TOKEN);
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰입니다: {}", e.getMessage());
            throw new CustomException(ErrorCode.INVALID_JWT_TOKEN);
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰의 클레임이 비어있습니다: {}", e.getMessage());
            throw new CustomException(ErrorCode.INVALID_JWT_TOKEN);
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessExpirationMs() {
        return accessExpirationMs;
    }
}
