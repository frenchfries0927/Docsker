package com.project.moduleservicecontent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.project.moduleservicecontent.dto.QuestionDTO;
import com.project.moduleservicecontent.dto.QuestionHistoryDTO;
import com.project.moduleservicecontent.dto.UserAnswerDTO;
import com.project.moduleservicecontent.entity.QuestionEntity;
import com.project.moduleservicecontent.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TodayQuestionService {
    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 M월 d일");

    /**
     * 오늘의 질문을 조회합니다.
     * @return 오늘의 질문 정보
     */
    public QuestionDTO getTodayQuestion(Long userId) {
        // userId를 String으로 변환
        String userIdStr = String.valueOf(userId);

        QuestionEntity questionEntity = questionRepository.findTodayQuestion(userIdStr)
                .orElseThrow(() -> new NoSuchElementException("오늘의 질문이 준비되지 않았습니다."));
        
        return convertToQuestionDTO(questionEntity, true);
    }

    /**
     * 사용자가 답변한 질문 이력을 조회합니다.
     * @param userId 사용자 ID
     * @return 질문 이력 목록
     */
    public List<QuestionHistoryDTO> getQuestionHistory(Long userId) {
        // userId를 String으로 변환
        String userIdStr = String.valueOf(userId);
        
        // 오늘 날짜까지 출판된 질문만 조회
        List<QuestionEntity> publishedQuestions = questionRepository.findQuestionsPublishedUntilToday();
        
        // 오늘 질문 조회
        QuestionEntity todayQuestion = questionRepository.findTodayQuestion(userIdStr).orElse(null);
        
        List<QuestionHistoryDTO> historyList = new ArrayList<>();
        
        // 오늘 질문이 있으면 가장 먼저 추가
        if (todayQuestion != null) {
            String userAnswer = getUserAnswerForQuestion(todayQuestion, userIdStr);
            String llmAnswer = getLlmAnswerForQuestion(todayQuestion, userIdStr);
            boolean isBookmarked = false;
            if (userId != null) {
                isBookmarked = todayQuestion.getBookmarkUsersList().contains(userId);
                System.out.println("isBookmarked = " + isBookmarked);
            }
            historyList.add(new QuestionHistoryDTO(
                    todayQuestion.getId(),
                    formatDate(todayQuestion.getPublishedAt().toLocalDate()),
                    todayQuestion.getQuestion(),
                    userAnswer,
                    llmAnswer,
                    true,
                    isBookmarked
            ));
        }
        
        // 오늘 날짜까지 출판된 질문 이력 추가 (최신순)
        for (QuestionEntity question : publishedQuestions) {
            // 오늘 질문은 이미 추가했으므로 건너뜀
            if (todayQuestion != null && question.getId().equals(todayQuestion.getId())) {
                continue;
            }
            
            String userAnswer = getUserAnswerForQuestion(question, userIdStr);
            String llmAnswer = getLlmAnswerForQuestion(question, userIdStr);
            boolean isBookmarked = false;
            if (userId != null) {
                isBookmarked = question.getBookmarkUsersList().contains(userId); // 🔥 여기!
                System.out.println("isBookmarked = " + isBookmarked);
            }
            historyList.add(new QuestionHistoryDTO(
                    question.getId(),
                    formatDate(question.getPublishedAt().toLocalDate()),
                    question.getQuestion(),
                    userAnswer,
                    llmAnswer,
                    false,
                    isBookmarked
            ));
        }
        
        return historyList;
    }

    /**
     * 특정 질문에 대한 사용자의 답변을 조회합니다.
     */
    private String getUserAnswerForQuestion(QuestionEntity question, String userId) {
        if (question.getAnswers() == null || question.getAnswers().isEmpty()) {
            return "";
        }
        
        try {
            JsonNode answersNode = objectMapper.readTree(question.getAnswers());
            
            // 단일 객체인지 배열인지 확인
            if (answersNode.isArray()) {
                for (int i = 0; i < answersNode.size(); i++) {
                    JsonNode answerNode = answersNode.get(i);
                    
                    if (answerNode.has("user_id")) {
                        // user_id 값을 문자열로 변환하여 비교
                        String nodeUserId = "";
                        JsonNode userIdNode = answerNode.get("user_id");
                        
                        if (userIdNode.isNumber()) {
                            nodeUserId = String.valueOf(userIdNode.asLong());
                        } else if (userIdNode.isTextual()) {
                            nodeUserId = userIdNode.asText();
                        }
                        
                        // 사용자 ID가 일치하는 경우
                        if (nodeUserId.equals(userId)) {
                            if (answerNode.has("answer")) {
                                return answerNode.get("answer").asText();
                            }
                        }
                    }
                }
            } else if (answersNode.isObject()) {
                // 단일 객체인 경우 (배열이 아닌)
                if (answersNode.has("user_id")) {
                    String nodeUserId = "";
                    JsonNode userIdNode = answersNode.get("user_id");
                    
                    if (userIdNode.isNumber()) {
                        nodeUserId = String.valueOf(userIdNode.asLong());
                    } else if (userIdNode.isTextual()) {
                        nodeUserId = userIdNode.asText();
                    }
                    
                    if (nodeUserId.equals(userId) && answersNode.has("answer")) {
                        return answersNode.get("answer").asText();
                    }
                }
            }
        } catch (JsonProcessingException e) {
            // 로그 남기기
            e.printStackTrace();
        }
        
        return "";
    }

    /**
     * 특정 질문에 대한 사용자의 LLM 답변을 조회합니다.
     */
    private String getLlmAnswerForQuestion(QuestionEntity question, String userId) {
        if (question.getAnswers() == null || question.getAnswers().isEmpty()) {
            return "";
        }
        
        try {
            JsonNode answersNode = objectMapper.readTree(question.getAnswers());
            
            // 단일 객체인지 배열인지 확인
            if (answersNode.isArray()) {
                for (int i = 0; i < answersNode.size(); i++) {
                    JsonNode answerNode = answersNode.get(i);
                    
                    if (answerNode.has("user_id")) {
                        // user_id 값을 문자열로 변환하여 비교
                        String nodeUserId = "";
                        JsonNode userIdNode = answerNode.get("user_id");
                        
                        if (userIdNode.isNumber()) {
                            nodeUserId = String.valueOf(userIdNode.asLong());
                        } else if (userIdNode.isTextual()) {
                            nodeUserId = userIdNode.asText();
                        }
                        
                        // 사용자 ID가 일치하는 경우
                        if (nodeUserId.equals(userId)) {
                            if (answerNode.has("llm_answer")) {
                                return answerNode.get("llm_answer").asText();
                            }
                        }
                    }
                }
            } else if (answersNode.isObject()) {
                // 단일 객체인 경우 (배열이 아닌)
                if (answersNode.has("user_id")) {
                    String nodeUserId = "";
                    JsonNode userIdNode = answersNode.get("user_id");
                    
                    if (userIdNode.isNumber()) {
                        nodeUserId = String.valueOf(userIdNode.asLong());
                    } else if (userIdNode.isTextual()) {
                        nodeUserId = userIdNode.asText();
                    }
                    
                    if (nodeUserId.equals(userId) && answersNode.has("llm_answer")) {
                        return answersNode.get("llm_answer").asText();
                    }
                }
            }
        } catch (JsonProcessingException e) {
            // 로그 남기기
            e.printStackTrace();
        }
        
        return "";
    }

    /**
     * 사용자 답변을 저장합니다.
     * @param questionId 질문 ID
     * @param userAnswerDTO 사용자 답변 정보
     * @return 저장된 질문 정보
     */
    @Transactional
    public QuestionDTO saveUserAnswer(Long questionId, UserAnswerDTO userAnswerDTO) {
        QuestionEntity questionEntity = questionRepository.findById(questionId)
                .orElseThrow(() -> new NoSuchElementException("질문을 찾을 수 없습니다: " + questionId));
        
        try {
            // 기존 answers JSON 파싱
            ArrayNode answersArrayNode;
            if (questionEntity.getAnswers() != null && !questionEntity.getAnswers().isEmpty()) {
                JsonNode answersNode = objectMapper.readTree(questionEntity.getAnswers());
                if (answersNode.isArray()) {
                    answersArrayNode = (ArrayNode) answersNode;
                } else {
                    answersArrayNode = objectMapper.createArrayNode();
                }
            } else {
                answersArrayNode = objectMapper.createArrayNode();
            }
            
            // userId를 문자열로 변환
            String dtoUserId = String.valueOf(userAnswerDTO.getUserId());
            
            // 기존 사용자 답변이 있는지 확인
            boolean userAnswerExists = false;
            
            for (int i = 0; i < answersArrayNode.size(); i++) {
                JsonNode answerNode = answersArrayNode.get(i);
                if (answerNode.has("user_id")) {
                    // user_id 값 추출
                    String nodeUserId = "";
                    JsonNode userIdNode = answerNode.get("user_id");
                    
                    if (userIdNode.isNumber()) {
                        nodeUserId = String.valueOf(userIdNode.asLong());
                    } else if (userIdNode.isTextual()) {
                        nodeUserId = userIdNode.asText();
                    }
                    
                    if (nodeUserId.equals(dtoUserId)) {
                        // 기존 답변 업데이트
                        ObjectNode updatedNode = (ObjectNode) answerNode;
                        
                        // answer가 있는 경우에만 업데이트
                        if (userAnswerDTO.getAnswer() != null) {
                            updatedNode.put("answer", userAnswerDTO.getAnswer());
                        }
                        
                        // llm_answer가 있는 경우에만 업데이트
                        if (userAnswerDTO.getLlmAnswer() != null) {
                            updatedNode.put("llm_answer", userAnswerDTO.getLlmAnswer());
                            // score도 함께 업데이트
                            if (userAnswerDTO.getScore() != null) {
                                updatedNode.put("score", userAnswerDTO.getScore());
                            }
                        }
                        
                        userAnswerExists = true;
                        break;
                    }
                }
            }
            
            // 기존 답변이 없으면 새로 추가
            if (!userAnswerExists) {
                ObjectNode newAnswer = objectMapper.createObjectNode();
                newAnswer.put("user_id", userAnswerDTO.getUserId());
                
                // answer가 있는 경우에만 추가
                if (userAnswerDTO.getAnswer() != null) {
                    newAnswer.put("answer", userAnswerDTO.getAnswer());
                }
                
                // llm_answer가 있는 경우에만 추가
                if (userAnswerDTO.getLlmAnswer() != null) {
                    newAnswer.put("llm_answer", userAnswerDTO.getLlmAnswer());
                    // score도 함께 추가
                    if (userAnswerDTO.getScore() != null) {
                        newAnswer.put("score", userAnswerDTO.getScore());
                    }
                }
                
                answersArrayNode.add(newAnswer);
            }
            
            // JSON 업데이트
            questionEntity.setAnswers(objectMapper.writeValueAsString(answersArrayNode));
            questionRepository.save(questionEntity);
            
            return convertToQuestionDTO(questionEntity, true);
            
        } catch (JsonProcessingException e) {
            throw new RuntimeException("답변 저장 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 사용자 답변을 수정합니다.
     * @param questionId 질문 ID
     * @param userAnswerDTO 사용자 답변 정보
     * @return 수정된 질문 정보
     */
    @Transactional
    public QuestionDTO updateUserAnswer(Long questionId, UserAnswerDTO userAnswerDTO) {
        QuestionEntity questionEntity = questionRepository.findById(questionId)
                .orElseThrow(() -> new NoSuchElementException("질문을 찾을 수 없습니다: " + questionId));

        // 오늘 질문인지 확인
        Optional<QuestionEntity> todayQuestionOpt = questionRepository.findTodayQuestion(userAnswerDTO.getUserId().toString());
        if (todayQuestionOpt.isEmpty() || !todayQuestionOpt.get().getId().equals(questionId)) {
            throw new IllegalStateException("오늘의 질문만 수정할 수 있습니다.");
        }

        try {
            // 기존 answers JSON 파싱
            ArrayNode answersArrayNode;
            if (questionEntity.getAnswers() != null && !questionEntity.getAnswers().isEmpty()) {
                JsonNode answersNode = objectMapper.readTree(questionEntity.getAnswers());
                if (answersNode.isArray()) {
                    answersArrayNode = (ArrayNode) answersNode;
                } else {
                    answersArrayNode = objectMapper.createArrayNode();
                }
            } else {
                answersArrayNode = objectMapper.createArrayNode();
            }

            // userId를 문자열로 변환
            String dtoUserId = String.valueOf(userAnswerDTO.getUserId());

            // 기존 사용자 답변 찾기
            boolean userAnswerExists = false;
            ObjectNode existingAnswer = null;

            for (int i = 0; i < answersArrayNode.size(); i++) {
                JsonNode answerNode = answersArrayNode.get(i);
                if (answerNode.has("user_id")) {
                    String nodeUserId = "";
                    JsonNode userIdNode = answerNode.get("user_id");

                    if (userIdNode.isNumber()) {
                        nodeUserId = String.valueOf(userIdNode.asLong());
                    } else if (userIdNode.isTextual()) {
                        nodeUserId = userIdNode.asText();
                    }

                    if (nodeUserId.equals(dtoUserId)) {
                        existingAnswer = (ObjectNode) answerNode;
                        userAnswerExists = true;
                        break;
                    }
                }
            }

            // 기존 답변이 있으면 업데이트
            if (userAnswerExists && existingAnswer != null) {
                // answer가 있는 경우에만 업데이트
                if (userAnswerDTO.getAnswer() != null) {
                    existingAnswer.put("answer", userAnswerDTO.getAnswer());
                }
                
                // llm_answer가 있는 경우에만 업데이트
                if (userAnswerDTO.getLlmAnswer() != null) {
                    existingAnswer.put("llm_answer", userAnswerDTO.getLlmAnswer());
                    // score도 함께 업데이트
                    if (userAnswerDTO.getScore() != null) {
                        existingAnswer.put("score", userAnswerDTO.getScore());
                    }
                } else if (userAnswerDTO.getAnswer() != null) {
                    // 사용자 답변만 업데이트하는 경우 llm_answer와 score를 null로 설정
                    existingAnswer.putNull("llm_answer");
                    existingAnswer.putNull("score");
                }
            } else {
                // 기존 답변이 없으면 새로 추가
                ObjectNode newAnswer = objectMapper.createObjectNode();
                newAnswer.put("user_id", userAnswerDTO.getUserId());
                
                // answer가 있는 경우에만 추가
                if (userAnswerDTO.getAnswer() != null) {
                    newAnswer.put("answer", userAnswerDTO.getAnswer());
                }
                
                // llm_answer가 있는 경우에만 추가
                if (userAnswerDTO.getLlmAnswer() != null) {
                    newAnswer.put("llm_answer", userAnswerDTO.getLlmAnswer());
                    // score도 함께 추가
                    if (userAnswerDTO.getScore() != null) {
                        newAnswer.put("score", userAnswerDTO.getScore());
                    }
                } else {
                    // 초기 답변 추가 시에는 llm_answer와 score를 null로 설정
                    newAnswer.putNull("llm_answer");
                    newAnswer.putNull("score");
                }
                
                answersArrayNode.add(newAnswer);
            }

            // JSON 업데이트
            questionEntity.setAnswers(objectMapper.writeValueAsString(answersArrayNode));
            questionRepository.save(questionEntity);

            return convertToQuestionDTO(questionEntity, true);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("답변 수정 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 특정 질문 상세 정보를 조회합니다.
     * @param questionId 질문 ID
     * @param userId 사용자 ID
     * @return 질문 상세 정보
     */
    public QuestionDTO getQuestionById(Long questionId, Long userId) {
        String userIdStr = String.valueOf(userId);
        QuestionEntity questionEntity = questionRepository.findById(questionId)
                .orElseThrow(() -> new NoSuchElementException("질문을 찾을 수 없습니다: " + questionId));
        
        // 오늘 날짜보다 미래의 질문인 경우 예외 발생
        LocalDate today = LocalDate.now();
        if (questionEntity.getPublishedAt().toLocalDate().isAfter(today)) {
            throw new NoSuchElementException("해당 질문은 아직 공개되지 않았습니다.");
        }
        
        // 오늘 질문인지 확인
        boolean isToday = false;
        Optional<QuestionEntity> todayQuestionOpt = questionRepository.findTodayQuestion(userIdStr);
        if (todayQuestionOpt.isPresent() && todayQuestionOpt.get().getId().equals(questionId)) {
            isToday = true;
        }
        
        return convertToQuestionDTO(questionEntity, isToday);
    }

    /**
     * Entity를 DTO로 변환합니다.
     */
    private QuestionDTO convertToQuestionDTO(QuestionEntity entity, boolean isToday) {
        return new QuestionDTO(
                entity.getId(),
                entity.getQuestion(),
                entity.getQuestionType(),
                formatDate(entity.getPublishedAt().toLocalDate()),
                entity.getAnswers(),
                isToday
        );
    }

    /**
     * 날짜를 포맷팅합니다.
     */
    private String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }
}
