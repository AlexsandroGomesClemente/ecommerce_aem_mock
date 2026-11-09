package com.ecommerce.security;

import com.ecommerce.domain.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Serviço de emissão e validação criptográfica Stateless de JWT (OAuth 2.0 / Bearer / HttpOnly Cookie).
 * Todas as claims essenciais são assinadas no token para evitar consultas ao PostgreSQL a cada requisição.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;
    private final String cookieName;
    private final boolean cookieSecure;

    public JwtService(
            @Value("${ecommerce.security.jwt-secret}") String secretHex,
            @Value("${ecommerce.security.jwt-expiration-ms:86400000}") long expirationMs,
            @Value("${ecommerce.security.cookie-name:ACCESS_TOKEN}") String cookieName,
            @Value("${ecommerce.security.cookie-secure:false}") boolean cookieSecure
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(secretHex);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
        this.cookieName = cookieName;
        this.cookieSecure = cookieSecure;
    }

    public String generateToken(UserEntity user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("name", user.getName())
                .claim("role", user.getRole())
                .claim("planTier", user.getPlanTier())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public Claims parseAndValidateClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public AuthenticatedUserPrincipal toPrincipal(Claims claims, String authSource) {
        Number uidNumber = claims.get("uid", Number.class);
        Long userId = uidNumber != null ? uidNumber.longValue() : 0L;
        return new AuthenticatedUserPrincipal(
                userId,
                claims.get("name", String.class),
                claims.getSubject(),
                claims.get("role", String.class),
                claims.get("planTier", String.class),
                claims.getId(),
                authSource
        );
    }

    public Instant extractExpiration(String token) {
        Claims claims = parseAndValidateClaims(token);
        return claims.getExpiration().toInstant();
    }

    public String extractJti(String token) {
        Claims claims = parseAndValidateClaims(token);
        return claims.getId();
    }

    public long getExpirationSeconds() {
        return expirationMs / 1000L;
    }

    public String getCookieName() {
        return cookieName;
    }

    public boolean isCookieSecure() {
        return cookieSecure;
    }
}

