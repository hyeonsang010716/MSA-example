package com.example.service_gateway.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private final JWTVerifier verifier;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.verifier = JWT.require(Algorithm.HMAC256(secret)).build();
    }

    public DecodedJWT verify(String token) {
        return verifier.verify(token);
    }
}