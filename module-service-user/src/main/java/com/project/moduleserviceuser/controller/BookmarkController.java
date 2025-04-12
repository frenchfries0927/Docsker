package com.project.moduleserviceuser.controller;

import com.project.moduleserviceuser.common.ApiResponse;
import com.project.moduleserviceuser.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//테스트 끝나면 required = false 를 삭제할것.!
@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    // ===== Content 북마크 =====

    @PostMapping("/content/{contentId}")
    public ResponseEntity<ApiResponse<?>> addContentBookmark(@RequestHeader(value = "X-User-ID", required = false) Long userId,
                                                             @PathVariable Long contentId) {
        bookmarkService.addContentBookmark(userId, contentId);
        return ResponseEntity.ok(ApiResponse.success("콘텐츠 북마크 추가 완료"));
    }

    @DeleteMapping("/content/{contentId}")
    public ResponseEntity<ApiResponse<?>> removeContentBookmark(@RequestHeader(value = "X-User-ID", required = false) Long userId,
                                                                @PathVariable Long contentId) {
        bookmarkService.removeContentBookmark(userId, contentId);
        return ResponseEntity.ok(ApiResponse.success("콘텐츠 북마크 삭제 완료"));
    }

    @GetMapping("/content")
    public ResponseEntity<ApiResponse<?>> getContentBookmarks(@RequestHeader(value = "X-User-ID", required = false) Long userId,
                                                              @RequestParam(required = false) String type) {
        System.out.println("콘텐츠 북마크 조회 요청");
        return ResponseEntity.ok(ApiResponse.success(bookmarkService.getUserContentBookmarks(userId, type), "콘텐츠 북마크 목록"));
    }

    // ===== Question 북마크 =====

    @PostMapping("/question/{questionId}")
    public ResponseEntity<ApiResponse<?>> addQuestionBookmark(@RequestHeader(value = "X-User-ID", required = false) Long userId,
                                                              @PathVariable Long questionId) {
        bookmarkService.addQuestionBookmark(userId, questionId);
        return ResponseEntity.ok(ApiResponse.success("질문 북마크 추가 완료"));
    }

    @DeleteMapping("/question/{questionId}")
    public ResponseEntity<ApiResponse<?>> removeQuestionBookmark(@RequestHeader(value = "X-User-ID", required = false) Long userId,
                                                                 @PathVariable Long questionId) {
        bookmarkService.removeQuestionBookmark(userId, questionId);
        return ResponseEntity.ok(ApiResponse.success("질문 북마크 삭제 완료"));
    }

    @GetMapping("/question")
    public ResponseEntity<ApiResponse<?>> getQuestionBookmarks(@RequestHeader(value = "X-User-ID", required = false) Long userId) {
        return ResponseEntity.ok(ApiResponse.success(bookmarkService.getUserQuestionBookmarks(userId), "질문 북마크 목록"));
    }
}
