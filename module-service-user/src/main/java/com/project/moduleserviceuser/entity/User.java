package com.project.moduleserviceuser.entity;

import com.project.moduleserviceuser.enums.UserRole;
import com.project.moduleserviceuser.enums.UserStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Setter
@Getter
@EntityListeners(AuditingEntityListener.class) // 자동 시간 설정
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String uid;

    @Column(unique = true, nullable = false)
    private String email;

    //소셜 로그인 경우 provider + providerId
    @Column(unique = true, nullable = false)
    private String username;

    //소셜 로그인 경우 null 처리.
    private String password;
    private String provider;
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus active;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;


    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String bookmarkContent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String bookmarkQuestion;

    @PrePersist
    public void prePersist() {
        if (this.uid == null) {
            this.uid = UUID.randomUUID().toString();
        }
        if (this.active == null) {
            this.active = UserStatus.ACTIVE;
        }
        if (this.role == null) {
            this.role = UserRole.USER; // 기본값 USER
        }
    }
}
