package com.example.service_user.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtProvider {

    private final Algorithm algorithm;
    private final long expiration;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration
    ) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.expiration = expiration;
    }

    public String createToken(String userId) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + expiration);

        return JWT.create()
                .withClaim("user_id", userId)
                .withIssuedAt(now)
                .withExpiresAt(expiresAt)
                .sign(algorithm);
    }
}