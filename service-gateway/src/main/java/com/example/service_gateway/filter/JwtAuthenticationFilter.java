package com.example.service_gateway.filter;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.service_gateway.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/service-user/login",
            "/api/service-user/register"
    );

    private final JwtUtil jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();

        if (PUBLIC_PATHS.contains(path)) {
            log.info("[PUBLIC] {} {}", method, path);
            return chain.filter(exchange);
        }

        if (path.equals("/api/service-user/logout")) {
            log.info("[LOGOUT] {} {}", method, path);
            exchange.getResponse().addCookie(
                    ResponseCookie.from("accessToken")
                            .path("/")
                            .maxAge(0)
                            .build()
            );
            return chain.filter(exchange);
        }

        String token = resolveToken(exchange.getRequest());
        if (token == null) {
            log.warn("[AUTH FAIL] {} {} - 토큰 없음", method, path);
            return unauthorized(exchange);
        }

        try {
            DecodedJWT jwt = jwtUtil.verify(token);
            String userId = jwt.getClaim("user_id").asString();

            if (userId == null) {
                log.warn("[AUTH FAIL] {} {} - user_id 클레임 없음", method, path);
                return unauthorized(exchange);
            }

            log.info("[AUTH OK] {} {} - userId={}", method, path, userId);

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", userId)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (JWTVerificationException e) {
            log.warn("[AUTH FAIL] {} {} - {}", method, path, e.getMessage());
            return unauthorized(exchange);
        }
    }

    private String resolveToken(ServerHttpRequest request) {
        String bearer = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
