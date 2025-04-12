package com.project.moduleservicecontent.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "contents")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentsEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "content_detail", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @JsonRawValue
    private String contentDetail;

    @Column(name = "views")
    private Integer views;

    @Column(name = "bookmark_users", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON) // Hibernate 가 JSON 타입을 인식하도록 설정
    private String bookmarkUsers;

    // Virtual column for UID from the content_detail JSON
    @Transient
    private String uid;

    @Transient
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Helper method to extract UID from content_detail
    public String getUid() {
        if (contentDetail != null && !contentDetail.isEmpty()) {
            try {
                JsonNode node = objectMapper.readTree(contentDetail);
                if (node.has("uid")) {
                    return node.get("uid").asText();
                }
            } catch (Exception e) {
                // 예외가 발생할 경우 로깅하거나 처리할 수 있습니다
            }
        }
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    // 추가: content_detail JSON에서 특정 필드를 추출하는 유틸리티 메서드
    public String getJsonField(String fieldName) {
        if (contentDetail != null && !contentDetail.isEmpty()) {
            try {
                JsonNode node = objectMapper.readTree(contentDetail);
                if (node.has(fieldName)) {
                    return node.get(fieldName).asText();
                }
            } catch (Exception e) {
                // 예외 처리
            }
        }
        return null;
    }

    public <T> T getJsonFieldAsObject(String fieldName, TypeReference<T> valueTypeRef) {
        if (contentDetail != null && !contentDetail.isEmpty()) {
            try {
                JsonNode node = objectMapper.readTree(contentDetail);
                JsonNode targetNode = node.get(fieldName);
                if (targetNode != null && !targetNode.isNull()) {
                    String jsonString = targetNode.toString();
                    return objectMapper.readValue(jsonString, valueTypeRef);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public List<Long> getBookmarkUsersList() {
        if (bookmarkUsers != null && !bookmarkUsers.isEmpty()) {
            try {
                return objectMapper.readValue(bookmarkUsers, new TypeReference<List<Long>>() {});
            } catch (Exception e) {
                // 파싱 예외 시 빈 리스트 반환
                return new ArrayList<>();
            }
        }
        return new ArrayList<>();
    }

    public void setBookmarkUsersList(List<Long> users) {
        try {
            this.bookmarkUsers = objectMapper.writeValueAsString(users);
        } catch (Exception e) {
            this.bookmarkUsers = "[]"; // 실패 시 빈 리스트로
        }
    }
    @PostConstruct
    public void init() {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}