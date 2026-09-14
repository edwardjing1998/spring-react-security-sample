package com.example.securityapi.security;

import com.example.securityapi.user.AppUser;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationSeconds;

    public JwtService(
            @Value("${app.jwt.secret}")
            String base64Secret,

            @Value("${app.jwt.expiration-seconds:900}")
            long expirationSeconds
    ) {
        byte[] keyBytes =
                Decoders.BASE64.decode(base64Secret);

        this.key =
                Keys.hmacShaKeyFor(keyBytes);

        this.expirationSeconds =
                expirationSeconds;
    }

    public String createToken(AppUser user) {
        Instant now = Instant.now();
        Instant expiresAt =
                now.plusSeconds(expirationSeconds);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim(
                        "role",
                        user.getRole().name()
                )
                .issuedAt(
                        Date.from(now)
                )
                .expiration(
                        Date.from(expiresAt)
                )
                /*
                 * Explicitly use HS256.
                 *
                 * Do not use only signWith(key), because
                 * JJWT may automatically select HS512
                 * when the signing key is long enough.
                 */
                .signWith(
                        key,
                        Jwts.SIG.HS256
                )
                .compact();
    }

    public String extractSubject(
            String token
    ) {
        return claims(token).getSubject();
    }

    public boolean isValid(
            String token,
            String email
    ) {
        if (
                token == null ||
                        token.isBlank() ||
                        email == null ||
                        email.isBlank()
        ) {
            return false;
        }

        Claims claims = claims(token);

        String subject =
                claims.getSubject();

        Date expiration =
                claims.getExpiration();

        return subject != null
                && email.equalsIgnoreCase(subject)
                && expiration != null
                && expiration.after(new Date());
    }

    private Claims claims(
            String token
    ) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long expirationSeconds() {
        return expirationSeconds;
    }
}