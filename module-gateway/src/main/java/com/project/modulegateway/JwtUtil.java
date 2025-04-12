package com.project.modulegateway;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    private final SecretKey secretKey;

    public JwtUtil(@Value("${spring.jwt.secret}") String secret) {
        System.out.println("✅ Loaded JWT Secret: " + secret);

        // User Service와 동일한 SecretKey 처리 방식 적용
        this.secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),
                Jwts.SIG.HS256.key().build().getAlgorithm());
    }

    /**
     * ✅ 토큰이 유효한지 확인
     */
    public boolean isValidToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            System.out.println("❌ JWT 토큰이 만료되었습니다: " + e.getMessage());
        } catch (MalformedJwtException e) {
            System.out.println("❌ 잘못된 형식의 JWT 토큰입니다: " + e.getMessage());
        } catch (SignatureException e) {
            System.out.println("❌ JWT 서명이 유효하지 않습니다: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ JWT 검증 중 오류 발생: " + e.getMessage());
        }
        return false;
    }

    /**
     * ✅ 토큰 만료 여부 확인
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * ✅ 토큰에서 Username 추출
     */
    public Long getUserId(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("userId", Long.class);
    }

    /**
     * ✅ 토큰에서 Role 추출
     */
    public String getRole(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }
}