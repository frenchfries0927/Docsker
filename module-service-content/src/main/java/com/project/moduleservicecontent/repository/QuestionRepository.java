package com.project.moduleservicecontent.repository;

import com.project.moduleservicecontent.entity.QuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<QuestionEntity, Long> {
    // 오늘 날짜로 출판된 질문 조회
    @Query(value = "SELECT * FROM question WHERE DATE(published_at) = CURRENT_DATE", nativeQuery = true)
    Optional<QuestionEntity> findTodayQuestion(@Param("userId") String userId);
    
    // 특정 사용자가 답변한 질문 목록 조회 (answers JSON에서 user_id로 필터링)
    @Query(value = "SELECT * FROM question WHERE " +
            "(answers::jsonb @> ANY(ARRAY[('{\"user_id\": ' || :userId || '}')::jsonb]) OR " +
            "answers::jsonb @> ANY(ARRAY[('{\"user_id\":\"' || :userId || '\"}')::jsonb])) " +
            "ORDER BY published_at DESC", nativeQuery = true)
    List<QuestionEntity> findQuestionsByUserId(@Param("userId") String userId);
    
    // 모든 질문 목록 조회 (최신순)
    @Query(value = "SELECT * FROM question ORDER BY published_at DESC", nativeQuery = true)
    List<QuestionEntity> findAllQuestionsOrdered();
    
    // 오늘 날짜까지 출판된 질문 목록 조회 (최신순)
    @Query(value = "SELECT * FROM question WHERE DATE(published_at) <= CURRENT_DATE ORDER BY published_at DESC", nativeQuery = true)
    List<QuestionEntity> findQuestionsPublishedUntilToday();
    
    // 특정 기간 내 출판된 질문 목록 조회
    @Query(value = "SELECT * FROM question WHERE published_at BETWEEN :startDate AND :endDate ORDER BY published_at DESC", nativeQuery = true)
    List<QuestionEntity> findQuestionsByPublishedDateBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
