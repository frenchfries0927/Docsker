package com.project.moduleservicecontent.service;

import com.project.moduleservicecontent.entity.QuestionEntity;
import com.project.moduleservicecontent.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionBookmarkService {

    private final QuestionRepository questionRepository;

    /**
     * Question 북마크 목록 조회 (List<Map<String, Object>> 형태)
     */
    public List<Map<String, Object>> getBookmarkedQuestions(List<Long> questionIds, Long userId) {
        List<QuestionEntity> questions = questionRepository.findAllById(questionIds);

        return questions.stream()
                .map(q -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("questionId", q.getId());
                    result.put("question", q.getQuestion());
                    result.put("answer", q.getAnswerByUserId(userId));
                    result.put("publishedAt", q.getPublishedAt() != null ? q.getPublishedAt().toString() : "");
                    return result;
                })
                .collect(Collectors.toList());
    }

    /**
     * Question 북마크 응답용 Map으로 변환
     */
    private Map<String, Object> mapToQuestionResponse(QuestionEntity question) {
        return Map.of(
                "questionId", question.getId(),
                "question", question.getQuestion() != null ? question.getQuestion() : "",
                "answer", question.getJsonField("answer") != null ? question.getJsonField("answer") : "",
                "publishedAt", question.getJsonField("publishDate") != null ? question.getJsonField("publishDate") : ""
        );
    }


    /**
     * Kafka Consumer에서 호출할 Question 북마크 추가
     */
    public void addBookmarkUser(Long questionId, Long userId) {
        QuestionEntity question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("질문 없음"));

        List<Long> bookmarkUsers = question.getBookmarkUsersList();

        if (!bookmarkUsers.contains(userId)) {
            bookmarkUsers.add(userId);
            question.setBookmarkUsersList(bookmarkUsers);
            questionRepository.save(question); // ✅ QuestionRepository로 저장
        }
    }

    /**
     * Kafka Consumer에서 호출할 Question 북마크 삭제
     */
    public void removeBookmarkUser(Long questionId, Long userId) {
        QuestionEntity question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("질문 없음"));

        List<Long> bookmarkUsers = question.getBookmarkUsersList();

        // 포함되어 있는 경우에만 삭제
        if (bookmarkUsers.contains(userId)) {
            bookmarkUsers.remove(userId);
            question.setBookmarkUsersList(bookmarkUsers);
            questionRepository.save(question);
        }
    }
}
