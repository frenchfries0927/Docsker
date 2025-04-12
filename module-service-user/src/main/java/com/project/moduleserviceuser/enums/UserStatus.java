package com.project.moduleserviceuser.enums;


import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum UserStatus {
    ACTIVE,      // 활성화된 계정
    INACTIVE,    // 이메일 인증 안 된 계정
    BANNED,      // 정지된 계정
    DELETED      // 탈퇴한 계정
}
