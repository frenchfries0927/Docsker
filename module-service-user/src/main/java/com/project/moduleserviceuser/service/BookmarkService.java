package com.project.moduleserviceuser.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.moduleserviceuser.dto.bookmark.*;
import com.project.moduleserviceuser.entity.User;
import com.project.moduleserviceuser.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BookmarkService {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ContentServiceClient contentFeignClient;
    private final BookmarkEventProducer bookmarkEventProducer;

    /**
     * 콘텐츠 북마크 추가
     */
    public void addContentBookmark(Long userId, Long contentId) {
        User user = getUser(userId);
        List<ContentBookmarkDTO> bookmarks = parseContentBookmarkList(user.getBookmarkContent());

        boolean alreadyBookmarked = bookmarks.stream()
                .anyMatch(b -> b.getContentId().equals(contentId));

        if (!alreadyBookmarked) {
            bookmarks.add(new ContentBookmarkDTO(contentId, LocalDateTime.now().toString()));
            user.setBookmarkContent(toJson(bookmarks));
            userRepository.save(user);
            bookmarkEventProducer.sendContentBookmarkEvent(userId, contentId);
        }
    }

    /**
     * 콘텐츠 북마크 삭제
     */
    public void removeContentBookmark(Long userId, Long contentId) {
        User user = getUser(userId);
        List<ContentBookmarkDTO> bookmarks = parseContentBookmarkList(user.getBookmarkContent());
        bookmarks.removeIf(b -> b.getContentId().equals(contentId));
        user.setBookmarkContent(toJson(bookmarks));
        userRepository.save(user);
        bookmarkEventProducer.sendContentBookmarkDeleteEvent(userId, contentId);
    }

    /**
     * 콘텐츠 북마크 조회
     */
    public List<BookmarkResponse> getUserContentBookmarks(Long userId, String typeFilter) {
        User user = getUser(userId);
        List<ContentBookmarkDTO> bookmarks = parseContentBookmarkList(user.getBookmarkContent());
        Map<Long, String> bookmarkCreatedAtMap = bookmarks.stream()
                .collect(Collectors.toMap(ContentBookmarkDTO::getContentId, ContentBookmarkDTO::getCreateAt));

        List<Long> contentIds = bookmarks.stream()
                .map(ContentBookmarkDTO::getContentId)
                .toList();

        List<Map<String, Object>> contents = contentFeignClient.getBookmarkedContents(contentIds);

        return contents.stream()
                .filter(content -> isTypeMatched(content, typeFilter))
                .map(content -> mapToBookmarkResponse(content, bookmarkCreatedAtMap.get(((Number) content.get("contentId")).longValue())))
//                .sorted(Comparator.comparing(BookmarkResponse::).reversed()) // 최신순 정렬
                .toList();
    }

    /**
     * Question 북마크 추가
     */
    public void addQuestionBookmark(Long userId, Long questionId) {
        User user = getUser(userId);
        List<QuestionBookmarkDTO> bookmarks = parseQuestionBookmarkList(user.getBookmarkQuestion());

        boolean alreadyBookmarked = bookmarks.stream()
                .anyMatch(b -> b.getQuestionId().equals(questionId));

        if (!alreadyBookmarked) {
            bookmarks.add(new QuestionBookmarkDTO(questionId, LocalDateTime.now().toString()));
            user.setBookmarkQuestion(toJson(bookmarks));
            userRepository.save(user);
            bookmarkEventProducer.sendQuestionBookmarkEvent(userId, questionId);
        }
    }

    /**
     * Question 북마크 삭제
     */
    public void removeQuestionBookmark(Long userId, Long questionId) {
        User user = getUser(userId);
        List<QuestionBookmarkDTO> bookmarks = parseQuestionBookmarkList(user.getBookmarkQuestion());
        bookmarks.removeIf(b -> b.getQuestionId().equals(questionId));
        user.setBookmarkQuestion(toJson(bookmarks));
        userRepository.save(user);
        bookmarkEventProducer.sendQuestionBookmarkDeleteEvent(userId, questionId);
    }

    /**
     * Question 북마크 조회
     */
    public List<QuestionBookmarkResponse> getUserQuestionBookmarks(Long userId) {
        User user = getUser(userId);
        List<QuestionBookmarkDTO> bookmarks = parseQuestionBookmarkList(user.getBookmarkQuestion());

        List<Long> questionIds = bookmarks.stream()
                .map(QuestionBookmarkDTO::getQuestionId)
                .toList();

        // Content Service로 질문 상세 요청
        List<Map<String, Object>> questions = contentFeignClient.getBookmarkedQuestions(questionIds, userId);
        questions.forEach(q -> System.out.println("✅ UserService 받은 question: " + q));
        Map<Long, String> createdAtMap = bookmarks.stream()
                .collect(Collectors.toMap(QuestionBookmarkDTO::getQuestionId, QuestionBookmarkDTO::getCreateAt));

        return questions.stream()
                .map(q -> QuestionBookmarkResponse.builder()
                        .questionId(((Number) q.get("questionId")).longValue())
                        .question((String) q.get("question"))
                        .answer((String) q.get("answer"))
                        .publishedAt((String) q.get("publishedAt"))
                        .build()
                ).toList();
    }


    /** 공통 메서드들 */
    private BookmarkResponse mapToBookmarkResponse(Map<String, Object> content, String createdAt) {
        String type = (String) content.get("type");

        if ("class".equals(type)) {
            return ClassBookmarkResponse.builder()
                    .contentId(((Number) content.get("contentId")).longValue())
                    .title((String) content.get("title"))
                    .type(type)
                    .module((String) content.get("module"))
                    .packageName((String) content.get("packageName"))
                    .createAt(createdAt)
                    .build();
        } else if ("TechBlog".equals(type)) {
            return TechBlogBookmarkResponse.builder()
                    .contentId(((Number) content.get("contentId")).longValue())
                    .title((String) content.get("title"))
                    .type(type)
                    .blogProvider((String) content.get("blogProvider"))
                    .summary((String) content.get("summary"))
                    .publishedDate((String) content.get("publishedDate"))
                    .link((String) content.get("link"))
                    .createAt(createdAt)
                    .build();
        }

        throw new IllegalArgumentException("알 수 없는 콘텐츠 타입: " + type);
    }

    private boolean isTypeMatched(Map<String, Object> content, String typeFilter) {
        return typeFilter == null || typeFilter.isEmpty() || typeFilter.equals(content.get("type"));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
    }

    private List<ContentBookmarkDTO> parseContentBookmarkList(String json) {
        try {
            if (json == null || json.isEmpty()) return new ArrayList<>();
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<QuestionBookmarkDTO> parseQuestionBookmarkList(String json) {
        try {
            if (json == null || json.isEmpty()) return new ArrayList<>();
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String toJson(Object list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }
}
