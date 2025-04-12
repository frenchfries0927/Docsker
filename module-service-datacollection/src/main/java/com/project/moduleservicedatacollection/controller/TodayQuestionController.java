package com.project.moduleservicedatacollection.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.moduleservicedatacollection.service.TodayQuestion.TodayQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/question")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://127.0.0.1:3000")
public class TodayQuestionController {
    private final TodayQuestionService todayQuestionService;

    @PostMapping("/create")
    public ResponseEntity<String> testQuestion() throws JsonProcessingException {
        String generateQuestions = todayQuestionService.createTodayQuestion();
        System.out.println(generateQuestions);
        todayQuestionService.saveTodayQuestions(generateQuestions);
        return ResponseEntity.ok(generateQuestions);
    }

    @GetMapping("/chat/stream")
    public SseEmitter streamChat(@RequestParam("chatdata") String chatdata) {
        System.out.println(chatdata);
        return todayQuestionService.createFeedbackTodayQuestion(chatdata);
    }



}
