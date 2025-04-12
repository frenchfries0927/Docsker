package com.project.moduleserviceuser.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@RedisHash(value = "refreshToken")
public class RefreshToken {

    @Id
    private String refreshToken; // KEY로 사용

    @CreatedDate
    private LocalDateTime createdAt;

    @TimeToLive
    private Long ttl;

    public static RefreshToken create(String refreshToken, long expirationMillis) {
        return RefreshToken.builder()
                .refreshToken(refreshToken)
                .createdAt(LocalDateTime.now())
                .ttl(TimeUnit.MILLISECONDS.toSeconds(expirationMillis)) // ms → s
                .build();
    }
}

