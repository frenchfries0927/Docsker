package com.project.moduleservicedatacollection.service.TodayQuestion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.moduleservicedatacollection.dto.TodayQuestion.GroqContentRequestDTO;
import com.project.moduleservicedatacollection.dto.TodayQuestion.ContentResponseDTO;
import com.project.moduleservicedatacollection.dto.TodayQuestion.MessageDTO;
import com.project.moduleservicedatacollection.entity.QuestionEntity;
import com.project.moduleservicedatacollection.repository.QuestionRepository;
import com.project.moduleservicedatacollection.service.AI.GroqAPI;
import com.project.moduleservicedatacollection.service.AI.HuggingFaceAPI;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TodayQuestionService {
    private final GroqAPI groqAPI;
    private final HuggingFaceAPI huggingFaceAPI;
    private final ObjectMapper objectMapper;
    private final QuestionRepository questionRepository;
    private WebClient webClient;

    @Autowired
    public void ChatController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://current-presently-mammal.ngrok-free.app/").build();
    }

    @Value("${groq.api.token}")
    private String groqAccessToken;

    public String createTodayQuestion() {
        MessageDTO systemPrompt = getQuestionGroqSystemPrompt();
        MessageDTO userPrompt = getQuestionGroqUserPrompt();
        List<MessageDTO> messageDTOList = new ArrayList<>();
        messageDTOList.add(systemPrompt);
        messageDTOList.add(userPrompt);

        GroqContentRequestDTO groqContentRequestDTO = new GroqContentRequestDTO();
        groqContentRequestDTO.setMessages(messageDTOList);
        groqContentRequestDTO.setModel("qwen-2.5-coder-32b");

        ContentResponseDTO contentResponseDTO = groqAPI.getGroqChat(groqContentRequestDTO, groqAccessToken, "application/json");
        return contentResponseDTO.getContent();
    }

    public void saveTodayQuestions(String llmResponse) throws JsonProcessingException {
       List<QuestionEntity> questionEntityList = objectMapper.readValue(llmResponse, new TypeReference<List<QuestionEntity>>() {});

       for (QuestionEntity questionEntity : questionEntityList) {

           questionEntity.setQuestion(questionEntity.getQuestion());
           questionEntity.setQuestionType(questionEntity.getQuestionType());
           questionEntity.setUpdatedAt(null);
           questionEntity.setPublishedAt(questionEntity.getPublishedAt());
           questionEntity.setAnswers(null);
           questionEntity.setBookmarkUsers("[]");
       }

       questionRepository.saveAll(questionEntityList);
    }
    
    public SseEmitter createFeedbackTodayQuestion(String chatdata) {
        SseEmitter emitter = new SseEmitter();

        // LM Studio API 호출을 위한 요청 본문 구성
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "llama-3-korean-bllossom-8b");
        // llama-3-korean-bllossom-8b
        // llama-3.2-korean-bllossom-3b

        // 시스템 메시지와 클라이언트에서 입력받은 chatdata를 user 메시지로 포함
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", getHFSystemTestPrompt());
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", chatdata);
        messages.add(systemMsg);
        messages.add(userMsg);
        requestBody.put("messages", messages);

        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 12000);
        requestBody.put("stream", true);

        // LM Studio의 /v1/chat/completions 엔드포인트에 POST 요청을 보냅니다.
        webClient.post()
                .uri("/v1/chat/completions")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToFlux(String.class)
                .subscribe(
                        data -> {
                            try {
                                // 받아온 각 데이터 청크를 클라이언트에 전송 (SSE 이벤트)
                                emitter.send(SseEmitter.event().data(data));
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        error -> emitter.completeWithError(error),
                        () -> emitter.complete()
                );

        return emitter;
    }

    @NotNull
    private static MessageDTO getQuestionGroqSystemPrompt() {
        MessageDTO systemPrompt = new MessageDTO();
        systemPrompt.setRole("system");
        systemPrompt.setContent("당신은 \"Question\" 테이블에 적재할 데이터를 생성하는 역할을 수행합니다. 아래 조건을 모두 만족하는 JSON 데이터를 생성해 주세요.\n\n" +
                "1. 출력 결과는 반드시 올바른 JSON 형식의 배열이어야 하며, 부가 설명이나 추가 문구(예: '아래는 요구 사항을 충족하는 JSON 형식의 배열입니다:' 등)는 전혀 포함하지 않아야 합니다.\n" +
                "2. 데이터 배열은 정확히 50개의 JSON 객체를 포함해야 합니다.\n" +
                "3. 각 JSON 객체는 다음 필드들을 포함해야 합니다:\n" +
                "   - question: 문자열 타입이며, 반드시 \"Java와 관련된 기본 지식 및 심화 지식에 대한 질문\"이 포함되어야 합니다.\n" +
                "   - question_type: 문자열 타입이며, \"Based\" (기본 지식) 또는 \"Advanced\" (심화 지식) 중 하나의 값을 가져야 합니다.\n" +
                "   - updatedAt: null\n" +
                "   - answers: null\n" +
                "   - publishedAt: 오늘 날짜부터 시작하여, 각 객체마다 하루씩 증가하는 날짜 값(예: \"2025-03-17T00:00:00\", \"2025-03-18T00:00:00\" 등)으로 설정되어야 합니다.\n" +
                "4. id 필드는 auto increment로 자동 저장되므로 포함하지 않아야 하며, createdAt 필드는 자동으로 생성되므로 별도로 포함하지 않아도 됩니다.\n" +
                "5. 최종 출력은 반드시 한국어로 번역되어야 합니다.\n" +
                "예시:\n" +
                "{\n" +
                "    \"question\": \"추상 클래스와 인터페이스의 차이는 무엇인가요?\",\n" +
                "    \"question_type\": \"Based\",\n" +
                "    \"updatedAt\": null,\n" +
                "    \"answers\": null,\n" +
                "    \"publishedAt\": \"2025-03-17T00:00:00\"\n" +
                "},\n" +
                "{\n" +
                "    \"question\": \"가비지 컬렉터 (Garbage Collector)의 주요 기능은 무엇인가요?\",\n" +
                "    \"question_type\": \"Based\",\n" +
                "    \"updatedAt\": null,\n" +
                "    \"answers\": null,\n" +
                "    \"publishedAt\": \"2025-03-18T00:00:00\"\n" +
                "}");
        return systemPrompt;
    }

    @NotNull
    private static MessageDTO getQuestionGroqUserPrompt() {
        MessageDTO userPrompt = new MessageDTO();
        userPrompt.setRole("user");
        userPrompt.setContent("아래 조건에 맞는 JSON 배열 데이터를 생성해 주세요. " +
                "출력은 반드시 오직 JSON 배열만 포함해야 하며, 추가적인 설명이나 예시 문구(예: '아래는 요구 사항을 충족하는 JSON 형식의 배열입니다:' 등)는 절대로 포함하지 않아야 합니다.\n" +
                "데이터 배열은 정확히 50개의 JSON 객체를 포함해야 합니다. \n" +
                "각 JSON 객체는 다음 필드들을 포함해야 합니다:\n" +
                "  - question: 문자열 타입이며, 반드시 'Java와 관련된 기본 지식 및 심화 지식에 대한 질문'이 포함되어야 합니다.\n" +
                "  - question_type: 'Based' 또는 'Advanced' 중 하나의 문자열 값이어야 합니다.\n" +
                "  - updatedAt: null\n" +
                "  - answers: null");
        return userPrompt;
    }

    @NotNull
    private static String getHFSystemTestPrompt() {
        return "당신은 개발자들이 제공한 기술 질문과 그에 대한 답변을 평가하는 전문 AI 평가자입니다.\\n"
                + "유저 프롬프트에는 질문과 답변이 모두 포함되어 있으나, 당신은 오직 질문에 대해 답변이 타당한지, 즉 질문의 요구사항에 부합하는지를 평가해야 합니다.\\n"
                + "평가 시 반드시 구체적인 평가 이유를 서술해야하고, 기술적 정확성, 완전성, 타당성, 그리고 추가 고려 사항을 반영해야 합니다.\\n\\n"
                + "유저 프롬프트 내 질문에 대해 답변이 기술적으로 정확하고, 질문의 요구사항에 적절히 대응하는지 평가합니다.\\n"
                + "답변이 질문에서 제시된 문제에 대해 구체적이고 충분한 설명을 제공하는지 확인합니다.\\n"
                + "답변에 누락되거나 보완해야 할 추가 요소를 명시합니다.\\n"
                + "1점부터 100점까지의 유효성 점수를 부여합니다.\\n"
                + "답변이 모호하거나 부정확할 경우 낮은 점수, 설명이 명확하고 기술적 근거가 확실할 경우 높은 점수를 부여합니다.\\n\\n"
                + "부가 설명이나 추가 문구는 전혀 포함하지 않아야 합니다.\\n"
                + "예시 유저 프롬프트:\\n"
                + "질문 : 자바에서 Stream API와 for 루프의 성능 차이는 어떤 상황에서 발생하나요?\\n"
                + "답변 : 데이터 크기가 작고 단순 반복만 필요한 경우 for 루프가 오버헤드 없이 빠르지만, 대용량 데이터나 병렬 처리가 필요한 경우 Stream API가 더 효율적입니다...\\n\\n"
                + "예시 답변 1:\\n"
                + "\"답변은 질문에서 요구한 '자바에서 Stream API와 for 루프의 성능 차이'에 대해 명확하게 설명하고, 각 상황(작은 데이터셋 vs 대용량 데이터)에 따른 성능 차이를 구체적인 기술 근거와 예제 코드를 통해 잘 제시하고 있습니다. 이에 따라 질문에 대한 답변의 타당성이 매우 높습니다.\"\\n"
                + "- 기술적 정확성 및 타당성: 우수 (각 상황에 대해 구체적인 기술 설명과 예제가 제공됨)\\n"
                + "- 성능 분석: 현실적인 시나리오와 데이터를 바탕으로 충분한 설명 제공\\n"
                + "- 최적화 고려 사항: 코드 예시 및 최적화 포인트가 명확하게 제시됨\\n"
                + "타당성 점수: 95/100\\n\\n"
                + "예시 답변 2:\\n"
                + "\"답변은 질문에서 요구하는 '자바에서 Stream API와 for 루프의 성능 차이'에 대해 다소 모호하게 서술되어 있으며, 구체적인 기술 근거나 예제 코드가 부족합니다. 이로 인해 질문의 요구사항에 완벽히 부합하지 못해 타당성이 낮습니다.\"\\n"
                + "- 기술적 정확성 및 타당성: 미흡 (답변이 불충분하거나 부정확함)\\n"
                + "- 성능 분석: 구체적 데이터나 예제가 부족하여 설명이 불충분함\\n"
                + "- 최적화 고려 사항: 추가적인 설명이나 개선 제안이 전혀 없음\\n"
                + "타당성 점수: 0/100\\n\\n"
                + "평가 결과는 반드시 다음과 같은 구조로 작성되어야 합니다:\\n\\n"
                + "\"답변은 질문에서 요구하는 '~~~'에 대해 ~~~ 서술되어 있으며, ~~~ 합니다. 따라서, ~~~ 하는 것이 좋겠습니다.\"\\n\\n"
                + "- 첫 번째 세부 평가 항목 (예: 질문에 대한 답변의 기술적 정확성 및 타당성)\\n"
                + "- 두 번째 세부 평가 항목 (예: 성능 분석 및 최적화 고려 사항에 대한 설명의 충분성)\\n"
                + "- 세 번째 세부 평가 항목 (예: 추가 고려 사항 및 개선점 제안)\\n\\n"
                + "타당성 점수: XX/100\\n\\n";
    }


//    private static String getHFSystemTestPrompt() {
//        return "귀하는 사용자 답변을 평가하는 전문 AI 평가자입니다. 여러분의 임무는 주어진 질문과 해당 사용자 답변을 분석하여 정확성, 완전성, 타당성을 평가하는 것입니다. 모든 답변은 한국어로 작성해야 합니다.\n\n" +
//                "평가 가이드라인은 다음과 같습니다:\n\n" +
//                "1. 사용자 답변의 정확성과 관련성을 평가합니다.\n" +
//                "2. 누락된 요소나 고려해야 할 추가 점수를 파악합니다.\n" +
//                "3. 1점부터 100점까지의 유효성 점수를 제공합니다.\n" +
//                "4. 평가가 객관적이고 건설적인지 확인하세요.\n";
////                "이 과정에는 질문과 사용자의 답변을 검토하는 것이 포함됩니다. 그런 다음 다음 필드가 포함된 JSON 형식으로 평가를 전달해야 합니다:\n\n" +
////                "1. 출력 결과는 반드시 올바른 JSON 형식의 배열이어야 하며, " +
////                "부가 설명이나 추가 문구(예: '아래는 요구 사항을 충족하는 JSON 형식의 배열입니다:' 등)는 전혀 포함하지 않아야 합니다. " +
////                "2. user_id: 이 필드는 null이어야 합니다.\n" +
////                "3. answer: 이 필드에는 사용자의 답변이 포함되어야 합니다.\n" +
////                "4. llm_answer: 이 필드에 평가를 작성합니다. 전반적인 평가로 시작한 다음 고려해야 할 추가 사항을 글머리 기호로 나열합니다.\n" +
////                "5. score: 이 필드에는 1에서 100 사이의 유효성 점수를 입력해야 합니다." +
////                "\"예시:\\n\" +\n" +
////                "\"{\\n\" +\n" +
////                "\"   \\\"user_id\\\" : \\\"User 테이블 id (type: integer)\\\", \\n\" +\n" +
////                "\"   \\\"answer\\\" : \\\"사용자가 입력한 답변\\\", \\n\" +\n" +
////                "\"   \\\"llm answer\\\" : \\\"llm이 사용자 답변을 보고 평가한 내용\\\",\\n\" +\n" +
////                "\"   \\\"score\\\" : \\\"llm이 평가한 점수\\\"\\n\" +\n" +
////                "\"}\";";
//    }
}
