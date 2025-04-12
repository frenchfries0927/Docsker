package com.project.moduleserviceuser.service;

import com.project.moduleserviceuser.common.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookmarkEventProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;

    // 북마크 추가 요청 - 콘텐츠
    public void sendContentBookmarkEvent(Long userId, Long contentId) {
        String message = userId + ":" + contentId;
        kafkaTemplate.send(KafkaTopics.CONTENT_BOOKMARK_TOPIC, message);
        System.out.println("Content Bookmark event sent to " + message);
    }

    // 북마크 삭제 요청 - 콘텐츠
    public void sendContentBookmarkDeleteEvent(Long userId, Long contentId) {
        String message = userId + ":" + contentId;
        kafkaTemplate.send(KafkaTopics.CONTENT_BOOKMARK_DELETE_TOPIC, message);
        System.out.println("Content Bookmark DELETE event sent to " + message);
    }

    // 북마크 추가 요청 - 질문
    public void sendQuestionBookmarkEvent(Long userId, Long questionId) {
        String message = userId + ":" + questionId;
        kafkaTemplate.send(KafkaTopics.QUESTION_BOOKMARK_TOPIC, message);
        System.out.println("Question Bookmark event sent to " + message);
    }

    // 북마크 삭제 요청 - 질문
    public void sendQuestionBookmarkDeleteEvent(Long userId, Long questionId) {
        String message = userId + ":" + questionId;
        kafkaTemplate.send(KafkaTopics.QUESTION_BOOKMARK_DELETE_TOPIC, message);
        System.out.println("Question Bookmark DELETE event sent to " + message);
    }

}
