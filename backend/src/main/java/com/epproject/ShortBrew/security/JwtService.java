package com.epproject.ShortBrew.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class JwtService {

    private final Algorithm algorithm;
    private final String issuer = "shortbrew";
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtService(
            @Value("${jwt.secret:default-secret-key-should-be-replaced-in-production-1234567890}") String secret,
            @Value("${jwt.access-token-expiration-ms:3600000}") long accessTokenExpirationMs,
            @Value("${jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs
    ) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String generateAccessToken(UUID userId) {
        return JWT.create()
                .withIssuer(issuer)
                .withSubject(userId.toString())
                .withClaim("type", "access")
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plusMillis(accessTokenExpirationMs))
                .sign(algorithm);
    }

    public String generateRefreshToken(UUID userId) {
        return JWT.create()
                .withIssuer(issuer)
                .withSubject(userId.toString())
                .withClaim("type", "refresh")
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plusMillis(refreshTokenExpirationMs))
                .sign(algorithm);
    }

    public UUID verifyAccessToken(String token) {
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .withClaim("type", "access")
                .build();
        DecodedJWT jwt = verifier.verify(token);
        return UUID.fromString(jwt.getSubject());
    }

    public UUID verifyRefreshToken(String token) {
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .withClaim("type", "refresh")
                .build();
        DecodedJWT jwt = verifier.verify(token);
        return UUID.fromString(jwt.getSubject());
    }
}
