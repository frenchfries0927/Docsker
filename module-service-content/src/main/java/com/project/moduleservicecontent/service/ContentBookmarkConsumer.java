package com.project.moduleservicecontent.service;

import com.project.moduleservicecontent.common.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentBookmarkConsumer {

    // 실제로 DB 업데이트를 위해 Service 주입
    private final ContentBookmarkService contentBookmarkService;
    private final QuestionBookmarkService questionBookmarkService;

    @KafkaListener(topics = KafkaTopics.CONTENT_BOOKMARK_TOPIC, groupId = "content-bookmark-group")
    public void consumeContentBookmark(ConsumerRecord<String, String> record) {
        log.info("✅ 콘텐츠 북마크 이벤트 수신: {}", record.value());

        try {
            // "userId:contentId" 형식 파싱
            String[] parts = record.value().split(":");
            Long userId = Long.parseLong(parts[0]);
            Long contentId = Long.parseLong(parts[1]);

            // Service 통해 Content Entity의 bookmarkUsers에 userId 추가
            contentBookmarkService.addBookmarkUser(contentId, userId);

            log.info("📌 콘텐츠 ID {}에 유저 ID {} 북마크 추가 완료", contentId, userId);
        } catch (Exception e) {
            log.error("❌ Kafka 메시지 파싱 실패: {}", record.value(), e);
        }
    }

    // 콘텐츠 북마크 삭제
    @KafkaListener(topics = KafkaTopics.CONTENT_BOOKMARK_DELETE_TOPIC, groupId = "content-bookmark-group")
    public void consumeContentBookmarkDelete(ConsumerRecord<String, String> record) {
        log.info("🗑️ 콘텐츠 북마크 삭제 이벤트 수신: {}", record.value());
        try {
            String[] parts = record.value().split(":");
            Long userId = Long.parseLong(parts[0]);
            Long contentId = Long.parseLong(parts[1]);

            contentBookmarkService.removeBookmarkUser(contentId, userId);
            log.info("❌ 콘텐츠 ID {}에서 유저 ID {} 북마크 삭제 완료", contentId, userId);
        } catch (Exception e) {
            log.error("❌ Kafka 삭제 메시지 파싱 실패: {}", record.value(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.QUESTION_BOOKMARK_TOPIC, groupId = "question-bookmark-group")
    public void consumeQuestionBookmark(ConsumerRecord<String, String> record) {
        log.info("✅ 질문 북마크 이벤트 수신: {}", record.value());

        try {
            // "userId:questionId" 형식 파싱
            String[] parts = record.value().split(":");
            Long userId = Long.parseLong(parts[0]);
            Long questionId = Long.parseLong(parts[1]);

            // Service 통해 Question Entity의 bookmarkUsers에 userId 추가
            questionBookmarkService.addBookmarkUser(questionId, userId);

            log.info("📌 질문 ID {}에 유저 ID {} 북마크 추가 완료", questionId, userId);
        } catch (Exception e) {
            log.error("❌ Kafka 메시지 파싱 실패: {}", record.value(), e);
        }
    }

    // 질문 북마크 삭제
    @KafkaListener(topics = KafkaTopics.QUESTION_BOOKMARK_DELETE_TOPIC, groupId = "question-bookmark-group")
    public void consumeQuestionBookmarkDelete(ConsumerRecord<String, String> record) {
        log.info("🗑️ 질문 북마크 삭제 이벤트 수신: {}", record.value());
        try {
            String[] parts = record.value().split(":");
            Long userId = Long.parseLong(parts[0]);
            Long questionId = Long.parseLong(parts[1]);

            questionBookmarkService.removeBookmarkUser(questionId, userId);
            log.info("❌ 질문 ID {}에서 유저 ID {} 북마크 삭제 완료", questionId, userId);
        } catch (Exception e) {
            log.error("❌ Kafka 삭제 메시지 파싱 실패: {}", record.value(), e);
        }
    }

}
