package com.project.moduleservicecontent.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private String questionType;

    @Column(name = "created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    // JSON 데이터를 문자열로 저장 (답변들)
    @Column(name = "answers", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String answers;

    // 북마크 유저 목록 JSON
    @Column(name = "bookmark_users", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String bookmarkUsers;

    // ObjectMapper static으로
    @Transient
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * JSON 필드 파싱 유틸
     */
    public String getJsonField(String fieldName) {
        if (answers != null && !answers.isEmpty()) {
            try {
                JsonNode node = objectMapper.readTree(answers);
                if (node.has(fieldName)) {
                    return node.get(fieldName).asText();
                }
            } catch (Exception e) {
                // 예외 처리
            }
        }
        return null;
    }

    /**
     * bookmark_users JSON → List<Long>
     */
    public List<Long> getBookmarkUsersList() {
        if (bookmarkUsers != null && !bookmarkUsers.isEmpty()) {
            try {
                return objectMapper.readValue(bookmarkUsers, new TypeReference<List<Long>>() {});
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }
        return new ArrayList<>();
    }

    /**
     * List<Long> → bookmark_users JSON
     */
    public void setBookmarkUsersList(List<Long> users) {
        try {
            this.bookmarkUsers = objectMapper.writeValueAsString(users);
        } catch (Exception e) {
            this.bookmarkUsers = "[]";
        }
    }

    public String getAnswerByUserId(Long userId) {
        if (answers != null && !answers.isEmpty()) {
            try {
                JsonNode node = objectMapper.readTree(answers);
                if (node.isArray()) {
                    for (JsonNode answerNode : node) {
                        if (answerNode.has("user_id") && answerNode.get("user_id").asLong() == userId) {
                            return answerNode.has("answer") ? answerNode.get("answer").asText() : "";
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    @PostConstruct
    public void init() {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
