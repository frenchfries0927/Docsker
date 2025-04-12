package com.project.moduleservicedatacollection.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "question")
public class QuestionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question", nullable = false)
    private String question;

    @Column(name = "question_type", nullable = false)
    @JsonProperty("question_type")
    private String questionType;

    @Column(name = "created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    // JSON 데이터를 문자열로 저장
    @Column(name = "answers", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String answers;

    @Column(name = "bookmark_users", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON) // Hibernate 가 JSON 타입을 인식하도록 설정
    private String bookmarkUsers;
}
