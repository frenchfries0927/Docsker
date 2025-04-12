package com.project.modulegateway;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config> {

    private final JwtUtil jwtUtil;
    private final AuthorizationFilterConfig filterConfig;

    @Autowired
    public AuthorizationHeaderFilter(JwtUtil jwtUtil, AuthorizationFilterConfig filterConfig) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
        this.filterConfig = filterConfig;
    }

    public static class Config {
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
             String path = request.getURI().getPath();
            boolean isOptional = filterConfig.getOptionalPaths().stream()
                    .anyMatch(path::startsWith);
            System.out.println("isOptional = " + isOptional);

            if (isOptional) {
                // Optional 경로 → 토큰 있어도 되고 없어도 됨
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    try {
                        if (jwtUtil.isValidToken(token) && !jwtUtil.isTokenExpired(token)) {
                            long userId = jwtUtil.getUserId(token);
                            String role = jwtUtil.getRole(token);
                            System.out.println("userId = " + userId);
                            ServerHttpRequest mutatedRequest = request.mutate()
                                    .header("X-User-ID", String.valueOf(userId))
                                    .header("X-User-Role", role)
                                    .build();
                            exchange = exchange.mutate().request(mutatedRequest).build();
                        }
                    } catch (Exception e) {
                        System.out.println("❗️ Optional JWT validation failed → 그냥 통과");
                    }
                }
                return chain.filter(exchange);
            }

            System.out.println("authHeader = " + authHeader);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                if (!jwtUtil.isValidToken(token)) {
                    return onError(exchange, "Invalid JWT Token", HttpStatus.UNAUTHORIZED);
                }

                if (jwtUtil.isTokenExpired(token)) {
                    return onError(exchange, "JWT Token is expired", HttpStatus.UNAUTHORIZED);
                }

                long userId = jwtUtil.getUserId(token);
                String role = jwtUtil.getRole(token);

                System.out.println("✅ User ID: " + userId);
                System.out.println("✅ Role: " + role);

                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-ID", String.valueOf(userId))
                        .header("X-User-Role", role)
                        .build();
                exchange = exchange.mutate().request(mutatedRequest).build();

            } catch (ExpiredJwtException e) {
                return onError(exchange, "JWT Token is expired", HttpStatus.UNAUTHORIZED);
            } catch (SignatureException e) {
                return onError(exchange, "Invalid JWT signature", HttpStatus.UNAUTHORIZED);
            } catch (Exception e) {
                return onError(exchange, "JWT validation failed", HttpStatus.UNAUTHORIZED);
            }

            return chain.filter(exchange);
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus status) {
        System.out.println("❌ " + err);
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }
}
