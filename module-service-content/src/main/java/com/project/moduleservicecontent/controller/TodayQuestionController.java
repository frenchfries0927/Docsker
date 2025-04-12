package com.project.moduleservicecontent.controller;

import com.project.moduleservicecontent.dto.QuestionDTO;
import com.project.moduleservicecontent.dto.QuestionHistoryDTO;
import com.project.moduleservicecontent.dto.UserAnswerDTO;
import com.project.moduleservicecontent.service.TodayQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class TodayQuestionController {
    private final TodayQuestionService todayQuestionService;

    /**
     * 오늘의 질문 조회
     * @param userId 사용자 ID
     * @return 오늘의 질문 정보
     */
    @GetMapping("/today")
    public ResponseEntity<?> getTodayQuestion(@RequestHeader("X-User-ID") Long userId) {
        try {
            QuestionDTO questionDTO = todayQuestionService.getTodayQuestion(userId);
            return ResponseEntity.ok(questionDTO);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류가 발생했습니다.");
        }
    }

    /**
     * 질문 이력 조회
     * @param userId 사용자 ID
     * @return 질문 이력 목록
     */
    @GetMapping("/history")
    public ResponseEntity<?> getQuestionHistory(@RequestHeader("X-User-ID") Long userId) {
        try {
            List<QuestionHistoryDTO> historyList = todayQuestionService.getQuestionHistory(userId);
            return ResponseEntity.ok(historyList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류가 발생했습니다.");
        }
    }

    /**
     * 특정 질문의 상세 정보 조회
     * @param userId 사용자 ID
     * @param questionId 질문 ID
     * @return 질문 상세 정보
     */
    @GetMapping("/{questionId}")
    public ResponseEntity<?> getQuestionById(
            @RequestHeader("X-User-ID") Long userId,
            @PathVariable("questionId") Long questionId) {
        try {
            QuestionDTO questionDTO = todayQuestionService.getQuestionById(questionId, userId);
            return ResponseEntity.ok(questionDTO);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류가 발생했습니다.");
        }
    }

    /**
     * 사용자 답변 저장
     */
    @PostMapping("/{questionId}/answer")
    public ResponseEntity<?> saveUserAnswer(
            @RequestHeader("X-User-ID") Long userId,
            @PathVariable("questionId") Long questionId,
            @RequestBody UserAnswerDTO userAnswerDTO) {
        try {
            userAnswerDTO.setUserId(userId);
            QuestionDTO questionDTO = todayQuestionService.saveUserAnswer(questionId, userAnswerDTO);
            return ResponseEntity.ok(questionDTO);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류가 발생했습니다.");
        }
    }
    
    /**
     * LLM 답변 저장
     */
    @PostMapping("/{questionId}/llm-answer")
    public ResponseEntity<?> saveLlmAnswer(
            @RequestHeader("X-User-ID") Long userId,
            @PathVariable("questionId") Long questionId,
            @RequestBody UserAnswerDTO userAnswerDTO) {
        try {
            userAnswerDTO.setUserId(userId);
            
            // answer 필드를 llm_answer로 처리하고 score 추출
            if (userAnswerDTO.getAnswer() != null) {
                String llmAnswer = userAnswerDTO.getAnswer();
                Integer score = userAnswerDTO.getScore();
                
                // score가 null이고 llmAnswer에 점수 정보가 있는 경우 추출
                if (score == null && llmAnswer.contains("타당성 점수:")) {
                    int scoreIndex = llmAnswer.lastIndexOf("타당성 점수:");
                    String scoreStr = llmAnswer.substring(scoreIndex);
                    scoreStr = scoreStr.replaceAll("[^0-9]", "");
                    if (!scoreStr.isEmpty()) {
                        score = Integer.parseInt(scoreStr);
                    }
                    // 점수 부분을 제외한 답변만 저장
                    llmAnswer = llmAnswer.substring(0, scoreIndex).trim();
                }
                
                UserAnswerDTO newAnswerDTO = new UserAnswerDTO();
                newAnswerDTO.setUserId(userId);
                newAnswerDTO.setLlmAnswer(llmAnswer);
                newAnswerDTO.setScore(score);
                
                QuestionDTO questionDTO = todayQuestionService.saveUserAnswer(questionId, newAnswerDTO);
                return ResponseEntity.ok(questionDTO);
            }
            
            return ResponseEntity.badRequest().body("LLM 답변이 없습니다.");
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류가 발생했습니다.");
        }
    }

    /**
     * 사용자 답변 수정
     */
    @PutMapping("/{questionId}/answer")
    public ResponseEntity<?> updateUserAnswer(
            @RequestHeader("X-User-ID") Long userId,
            @PathVariable("questionId") Long questionId,
            @RequestBody UserAnswerDTO userAnswerDTO) {
        try {
            userAnswerDTO.setUserId(userId);
            QuestionDTO questionDTO = todayQuestionService.updateUserAnswer(questionId, userAnswerDTO);
            return ResponseEntity.ok(questionDTO);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류가 발생했습니다.");
        }
    }
}
