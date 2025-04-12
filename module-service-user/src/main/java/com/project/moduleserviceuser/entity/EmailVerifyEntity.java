package com.project.moduleserviceuser.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;


@RedisHash("email_verify")
@NoArgsConstructor
@Getter
public class EmailVerifyEntity {

    @Id
    private String email;

    private String code;

    @TimeToLive
    private long ttl; // TTL을 설정하여 자동 만료

    public EmailVerifyEntity(String email, String code) {
        this.email = email;
        this.code = code;
        this.ttl = 300; // 300초(5분) 후 자동 삭제
    }
}
