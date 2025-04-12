package com.project.moduleservicecontent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.moduleservicecontent.dto.BookmarkContentResponseDTO;
import com.project.moduleservicecontent.dto.BookmarkResponse;
import com.project.moduleservicecontent.service.ContentBookmarkService;
import com.project.moduleservicecontent.service.QuestionBookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


//Feign Client를 통한 데이터 전달을 위해 User Service에서 호출 하는 컨트롤러
@RestController
@RequestMapping("/contents")
@RequiredArgsConstructor
public class BookmarkController {

    private final ContentBookmarkService contentService;
    private final QuestionBookmarkService questionService;

    @PostMapping("/bookmarks/content")
    public ResponseEntity<List<Map<String, Object>>> getBookmarkedContents(@RequestParam("contentIds") List<Long> contentIds) {
        List<Map<String, Object>> contents = contentService.getBookmarkedContents(contentIds);

        // 디버깅용
        contents.forEach(content -> System.out.println("Content = " + content));

        return ResponseEntity.ok(contents);
    }

    @PostMapping("/bookmarks/questions")
    public ResponseEntity<List<Map<String, Object>>> getBookmarkedQuestions(@RequestParam("questionIds") List<Long> questionIds, @RequestParam("userId") Long userId) {
        List<Map<String, Object>> questions = questionService.getBookmarkedQuestions(questionIds, userId);
        questions.forEach(question -> System.out.println("Question = " + question));

        return ResponseEntity.ok(questions);
    }
}

