package com.project.moduleservicedatacollection.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@Table(name = "Contents")
@NoArgsConstructor
@AllArgsConstructor
public class ContentsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    // JSON 데이터를 문자열로 저장 (hibernate-types를 사용하면 JsonNode 등으로 매핑 가능)
    @Column(name = "content_detail", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON) // Hibernate 가 JSON 타입을 인식하도록 설정
    private String contentDetail;

    @Column(name = "created_at", nullable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(name = "views")
    private Integer views = 0;

    @Column(name = "bookmark_users", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON) // Hibernate 가 JSON 타입을 인식하도록 설정
    private String bookmarkUsers = "[]"; // 기본값으로 빈 JSON 배열 설정
}
