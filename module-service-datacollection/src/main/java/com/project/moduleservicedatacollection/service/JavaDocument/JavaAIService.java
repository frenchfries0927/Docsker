package com.project.moduleservicedatacollection.service.JavaDocument;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.project.moduleservicedatacollection.dto.JavaDocument.JavaClassDTO;
import com.project.moduleservicedatacollection.dto.TodayQuestion.MessageDTO;
import com.project.moduleservicedatacollection.entity.ContentsEntity;
import com.project.moduleservicedatacollection.repository.ContentsRepository;
import com.project.moduleservicedatacollection.service.AI.GroqAPI;
import com.project.moduleservicedatacollection.component.GroqHttpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
public class JavaAIService {
    private final GroqAPI groqAPI;
    private final ContentsRepository contentsRepository;
    private final ObjectMapper objectMapper;

    @Value("${groq.api.token}")
    private String groqAccessToken;

    /*
     * Groq Batch API를 이용하여 모듈 내 모든 클래스에 대한 methodExample을 생성하는 예시.
     */

    public void insertMethodExampleBatch(String moduleName) throws Exception {
        // 1. 모듈 엔티티 조회 (content_type = 'module'
        ContentsEntity moduleEntity = contentsRepository.findModuleName(moduleName);
        if(moduleEntity == null) {
            throw new RuntimeException("Module does not exist");
        }
        JsonNode moduleJson = objectMapper.readTree(moduleEntity.getContentDetail());
        ArrayNode packageArray = (ArrayNode) moduleJson.get("packages");
        if(packageArray == null || packageArray.isEmpty()) {
            throw new RuntimeException("Package is not found");
        }

        System.out.println("moduleEntity = " + moduleEntity);
        System.out.println("moduleJson = " + moduleJson);
        System.out.println("packageArray = " + packageArray);

        // 2. 각 클래스의 각 메서드에 대해 custom_id를 "classUid_index" 형식으로 지정하여 배치 요청 생성
        List<String> batchRequests = new ArrayList<>();
        for (JsonNode packageUidNode : packageArray) {
            String packageUid = packageUidNode.asText();
            ContentsEntity packageEntity = contentsRepository.findPackageByUid(packageUid);
            if(packageEntity == null) {
                System.err.println("Package " + packageUid + " does not exist");
                continue;
            }
            JsonNode packageJson = objectMapper.readTree(packageEntity.getContentDetail());
            ArrayNode classesArray = (ArrayNode) packageJson.get("classes");
            if(classesArray == null || classesArray.isEmpty()) {
                System.err.println("Classes is not found");
                continue;
            }

            System.out.println("packageEntity = " + packageEntity);
            System.out.println("packageJson = " + packageJson);
            System.out.println("classesArray = " + classesArray);

            for(JsonNode classUidNode : classesArray) {
                String classUid = classUidNode.asText();
                ContentsEntity classEntity = contentsRepository.findClassByUid(classUid);
                if(classEntity == null) {
                    System.err.println("Class " + classUid + " does not exist");
                    continue;
                }
                String classContentJson = classEntity.getContentDetail();

                // 기존에 classContentJson 전체를 보내던 부분을 아래와 같이 간소화합니다.
                JavaClassDTO javaClassDTO = objectMapper.readValue(classContentJson, JavaClassDTO.class);
                ObjectNode simplifiedClassNode = objectMapper.createObjectNode();
                simplifiedClassNode.put("uid", javaClassDTO.getUid());
                simplifiedClassNode.put("name", javaClassDTO.getName());
                simplifiedClassNode.put("type", javaClassDTO.getType());
                simplifiedClassNode.put("description", javaClassDTO.getDescription());
                // methods는 List 타입이므로, ArrayNode로 변환
                ArrayNode methodsNode = objectMapper.valueToTree(javaClassDTO.getMethods());
                simplifiedClassNode.set("methods", methodsNode);

                // 필요한 정보만 담긴 JSON 문자열 생성
                String simplifiedJson = simplifiedClassNode.toString();
                System.out.println("simplifiedJson = " + simplifiedJson);

                // AI에게 전달할 메시지 생성 시, 전체 classContentJson 대신 간소화된 정보를 전달
                MessageDTO systemMsg = getGroqSystemPrompt(simplifiedJson);
                MessageDTO userMsg = getGroqUserDTOS(simplifiedJson);

                // JSON 요청 본문 생성 (기존 코드와 동일)
                ObjectNode requestBody = objectMapper.createObjectNode();
                requestBody.put("model", "llama-3.3-70b-versatile");
                ArrayNode messages = objectMapper.createArrayNode();
                messages.add(objectMapper.valueToTree(systemMsg));
                messages.add(objectMapper.valueToTree(userMsg));
                requestBody.set("messages", messages);

                System.out.println("requestBody = " + requestBody);

                // 클래스 내 methods 배열의 크기만큼 반복하여 각 메서드에 대해 요청 생성
                ObjectNode classContentNode = (ObjectNode) objectMapper.readTree(classContentJson);
                ArrayNode methodsArray = (ArrayNode) classContentNode.get("methods");
                if (methodsArray == null || methodsArray.isEmpty()) {
                    System.err.println("methods 배열이 없는 클래스 uid: " + classUid);
                    continue;
                }

                for (int i = 0; i < methodsArray.size(); i++) {
                    // custom_id에 메서드 인덱스 추가: "classUid_index"
                    String customId = classUid + "_" + i;
                    ObjectNode batchRequest = objectMapper.createObjectNode();
                    batchRequest.put("custom_id", customId);
                    batchRequest.put("method", "POST");
                    batchRequest.put("url", "/v1/chat/completions");
                    batchRequest.set("body", requestBody);

                    String jsonLine = objectMapper.writeValueAsString(batchRequest);
                    batchRequests.add(jsonLine);
                }
            }
        }

        // 3. JSONL 파일로 저장
        String batchFilePath = "batch_requests.jsonl";
        Files.write(Paths.get(batchFilePath), batchRequests, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        System.out.println("Batch 요청 파일 생성: " + batchFilePath);

        // 4. 파일 업로드 및 배치 작업 생성
        String fileId = GroqHttpUtil.uploadFile(groqAccessToken, batchFilePath, "batch");
        System.out.println("업로드된 파일 ID: " + fileId);
        String batchId = GroqHttpUtil.createBatchJob(groqAccessToken, fileId, "/v1/chat/completions", "24h");
        System.out.println("생성된 배치 작업 ID: " + batchId);

        // 5. 배치 작업 완료까지 폴링 (최대 10분, 10초 간격)
        BatchStatusResult statusResult = waitForBatchCompletion(groqAccessToken, batchId, 10, TimeUnit.MINUTES);
        if (!"completed".equals(statusResult.getStatus())) {
            throw new RuntimeException("배치 작업이 성공적으로 완료되지 않았습니다. 상태: " + statusResult.getStatus());
        }
        String outputFileId = statusResult.getOutputFileId();
        String batchOutputFilePath = "batch_output.jsonl";
        GroqHttpUtil.downloadFile(groqAccessToken, "https://api.groq.com/openai/v1/files/" + outputFileId + "/content", batchOutputFilePath);
        System.out.println("배치 결과 파일 다운로드 완료: " + batchOutputFilePath);

        // 6. 결과 파일 파싱 후, 각 클래스의 methods 배열 순서대로 methodExample 업데이트
        updateClassMethodExamplesInOrder(batchOutputFilePath, packageArray);
    }

    /*
     * 결과 파일(batch_output.jsonl)을 읽어, 각 요청의 custom_id가 "classUid_index" 형식이라 가정하고,
     * 이를 그룹화한 후, 각 클래스의 methods 배열 순서대로 methodExample을 업데이트합니다.
     */

    public void updateClassMethodExamples(String batchOutputFilePath, String moduleName) throws Exception {
        // moduleName으로 모듈 엔티티 조회
        ContentsEntity moduleEntity = contentsRepository.findModuleName(moduleName);
        if (moduleEntity == null) {
            throw new RuntimeException("Module [" + moduleName + "] 을(를) 찾을 수 없습니다.");
        }
        // 모듈의 contentDetail에서 packageArray 추출
        JsonNode moduleJson = objectMapper.readTree(moduleEntity.getContentDetail());
        ArrayNode packageArray = (ArrayNode) moduleJson.get("packages");
        if (packageArray == null || packageArray.isEmpty()) {
            throw new RuntimeException("모듈 [" + moduleName + "] 내에 패키지 정보가 없습니다.");
        }
        // 기존 updateClassMethodExamplesInOrder 메서드를 호출
        updateClassMethodExamplesInOrder(batchOutputFilePath, packageArray);
    }

    private void updateClassMethodExamplesInOrder(String batchOutputFilePath, ArrayNode packageArray) throws Exception {
        List<String> outputLines = Files.readAllLines(Paths.get(batchOutputFilePath));
        // 그룹: key = classUid, value = TreeMap<index, exampleValue>
        Map<String, TreeMap<Integer, String>> examplesByClass = new HashMap<>();

        for (String line : outputLines) {
            if (line.trim().isEmpty()) continue;
            try {
                JsonNode responseNode = objectMapper.readTree(line);
                if (responseNode == null || responseNode.isNull()) continue;
                String customId = responseNode.get("custom_id").asText();
                // custom_id expected to be in "classUid_index" format
                String[] parts = customId.split("_");
                if (parts.length != 2) {
                    System.err.println("잘못된 custom_id 형식: " + customId);
                    continue;
                }
                String classUid = parts[0];
                int index;
                try {
                    index = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    System.err.println("인덱스 파싱 실패: " + customId);
                    continue;
                }
                JsonNode responseBody = responseNode.get("response").get("body");
                if (responseBody != null && responseBody.has("choices") &&
                        responseBody.get("choices").isArray() && responseBody.get("choices").size() > 0) {
                    String contentStr = responseBody.get("choices").get(0).get("message").get("content").asText();
                    JsonNode root;
                    try {
                        root = objectMapper.readTree(contentStr);
                    } catch (JsonProcessingException e) {
                        System.err.println("응답 JSON 파싱 실패, custom_id: " + customId + ", 에러: " + e.getMessage());
                        continue;
                    }
                    ArrayNode exampleArray;
                    if (root.isArray()) {
                        exampleArray = (ArrayNode) root;
                    } else if (root.isObject()) {
                        // 배열이 아닌 객체인 경우 배열로 감싸서 처리
                        exampleArray = objectMapper.createArrayNode();
                        exampleArray.add(root);
                    } else {
                        System.err.println("응답 JSON 형식이 배열 또는 객체가 아님, custom_id: " + customId);
                        continue;
                    }
                    if (exampleArray.size() > 0) {
                        JsonNode firstObj = exampleArray.get(0);
                        if (firstObj.has("example")) {
                            String exampleValue = firstObj.get("example").asText();
                            examplesByClass.computeIfAbsent(classUid, k -> new TreeMap<>()).put(index, exampleValue);
                        } else {
                            System.err.println("응답 객체에 'example' 키가 없음, custom_id: " + customId);
                        }
                    } else {
                        System.err.println("응답 배열이 비어 있음, custom_id: " + customId);
                    }
                }
            } catch (Exception e) {
                System.err.println("전체 응답 처리 중 오류: " + e.getMessage());
            }
        }

        // 각 패키지의 클래스를 순회하며 methods 배열 업데이트
        for (JsonNode packageUidNode : packageArray) {
            String packageUid = packageUidNode.asText();
            ContentsEntity packageEntity = contentsRepository.findPackageByUid(packageUid);
            if (packageEntity == null) continue;
            JsonNode packageJson = objectMapper.readTree(packageEntity.getContentDetail());
            ArrayNode classesArray = (ArrayNode) packageJson.get("classes");
            if (classesArray == null) continue;
            for (JsonNode classUidNode : classesArray) {
                String classUid = classUidNode.asText();
                if (!examplesByClass.containsKey(classUid)) {
                    System.err.println("응답 데이터가 없는 클래스 uid: " + classUid);
                    continue;
                }
                ContentsEntity classEntity = contentsRepository.findClassByUid(classUid);
                if (classEntity == null) continue;
                ObjectNode classContentNode = (ObjectNode) objectMapper.readTree(classEntity.getContentDetail());
                ArrayNode methodsArray = (ArrayNode) classContentNode.get("methods");
                if (methodsArray == null || methodsArray.isEmpty()) {
                    System.err.println("methods 배열이 없는 클래스 uid: " + classUid);
                    continue;
                }
                TreeMap<Integer, String> examplesMap = examplesByClass.get(classUid);
                for (int i = 0; i < methodsArray.size(); i++) {
                    if (examplesMap.containsKey(i)) {
                        ObjectNode methodNode = (ObjectNode) methodsArray.get(i);
                        methodNode.put("methodExample", examplesMap.get(i));
                    } else {
                        System.err.println("클래스 uid " + classUid + "의 인덱스 " + i + "에 대한 응답 없음");
                    }
                }
                classEntity.setContentDetail(objectMapper.writeValueAsString(classContentNode));
                contentsRepository.save(classEntity);
            }
        }
    }






    private MessageDTO getGroqSystemPrompt(String javaClassString) {
        MessageDTO systemMessageDTO = new MessageDTO();
        systemMessageDTO.setRole("system");
        String systemMessage = "당신은 Java Class or Interface의 Method의 대표적인 사용 예시 데이터를 생성하는 역할을 수행합니다. " +
                "아래 조건을 모두 만족하는 JSON 데이터를 생성해 주세요. " +
                "1. 출력 결과는 반드시 올바른 JSON 형식의 배열이어야 하며, " +
                "부가 설명이나 추가 문구(예: '아래는 요구 사항을 충족하는 JSON 형식의 배열입니다:' 등)는 전혀 포함하지 않아야 합니다. " +
                "2. 최종 출력은 반드시 한국어로 번역되어야 합니다." +
                "입력된 클래스 및 메서드 목록: " + javaClassString + "\n" +
                "3. 각 JSON 객체는 다음 필드들을 포함해야 합니다:\n" +
                "   - example: 문자열 타입이며, 메서드 목록에서 조회된 메서드 명 순서대로 받은 메서드의 대표적인 사용 예시를 코드 형태로 제공되어야 합니다." +
                "예시:\n" +
                "{\n" +
                "    \"example\": \"String str = \\\"Hello\\\";\\nint length = str.length(); // 결과: 5\",\n" +
                "},\n" +
                "{\n" +
                "    \"example\": \"String str = \\\"Hello\\\";\\nchar ch = str.charAt(0); // 결과: 'H'\",\n" +
                "}";
        systemMessageDTO.setContent(systemMessage);
        return systemMessageDTO;
    }

    private MessageDTO getGroqUserDTOS(String methods) {
        MessageDTO userMessageDTO = new MessageDTO();
        userMessageDTO.setRole("user");
        String userMessage = "아래 조건에 맞는 JSON 배열 데이터를 생성해 주세요. " +
                "출력은 반드시 오직 JSON 배열만 포함해야 하며, 추가적인 설명이나 예시 문구는 포함하면 안됩니다. " +
                "입력된 메서드 목록: " + methods + "\n" +
                "각 JSON 객체는 다음 필드를 포함해야 합니다:\n" +
                "   - example: 문자열 타입으로, 메서드의 대표 사용 예시 코드";
        userMessageDTO.setContent(userMessage);
        return userMessageDTO;
    }

    // Batch Job 상태를 폴링하는 헬퍼 메서드 (지정한 시간동안 주기적으로 상태 확인)
    private BatchStatusResult waitForBatchCompletion(String apiKey, String batchId, long timeout, TimeUnit unit) throws Exception {
        long timeoutMillis = unit.toMillis(timeout);
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            JsonNode response = GroqHttpUtil.getJson("https://api.groq.com/openai/v1/batches/" + batchId, apiKey);
            String status = response.get("status").asText();
            if ("completed".equals(status)) {
                String outputFileId = response.get("output_file_id").asText();
                return new BatchStatusResult(status, outputFileId);
            }
            // 10초 간격으로 폴링
            Thread.sleep(10000);
        }
        throw new RuntimeException("Batch job did not complete within timeout");
    }

    // 단순 데이터 클래스: 배치 상태 결과
    private static class BatchStatusResult {
        private final String status;
        private final String outputFileId;
        public BatchStatusResult(String status, String outputFileId) {
            this.status = status;
            this.outputFileId = outputFileId;
        }
        public String getStatus() { return status; }
        public String getOutputFileId() { return outputFileId; }
    }




    
//    // 레이트 리밋 관리를 위한 변수들
//    private final AtomicInteger requestsInCurrentMinute = new AtomicInteger(0);
//    private final AtomicInteger tokensInCurrentMinute = new AtomicInteger(0);
//    private volatile LocalDateTime currentMinuteStart = LocalDateTime.now();
//
//    // 스레드 풀 설정 - 코어 풀 크기 조정 가능
//    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
//
//    // 세마포어를 사용하여 동시 요청 수 제한 (RPM 500을 고려하여 초당 최대 8개 요청으로 제한)
//    private final Semaphore requestRateLimiter = new Semaphore(8);
//
//    /**
//     * 모든 모듈의 클래스들에 대해 methodExample 생성 작업을 수행합니다.
//     */
//    public void insertMethodExampleForAllModules() throws InterruptedException {
//        // 모든 모듈 조회
//        List<ContentsEntity> allModules = contentsRepository.findAllModules();
//        if (allModules == null || allModules.isEmpty()) {
//            throw new RuntimeException("모듈 정보가 없습니다.");
//        }
//
//        System.out.println("총 " + allModules.size() + "개의 모듈을 처리합니다.");
//
//        // 모듈마다 비동기로 처리
//        CountDownLatch modulesCompleted = new CountDownLatch(allModules.size());
//        List<CompletableFuture<Void>> moduleFutures = new ArrayList<>();
//
//        for (ContentsEntity moduleEntity : allModules) {
//            try {
//                // contentDetail에서 모듈 이름 추출
//                JsonNode moduleJson = objectMapper.readTree(moduleEntity.getContentDetail());
//                String moduleName = moduleJson.get("name").asText();
//
//                // 각 모듈을 비동기로 처리
//                CompletableFuture<Void> moduleFuture = CompletableFuture.runAsync(() -> {
//                    try {
//                        System.out.println("모듈 [" + moduleName + "] 처리 시작");
//                        insertMethodExample(moduleName);
//                        System.out.println("모듈 [" + moduleName + "] 처리 완료");
//                    } catch (Exception e) {
//                        System.err.println("모듈 [" + moduleName + "] 처리 중 오류: " + e.getMessage());
//                        e.printStackTrace();
//                    } finally {
//                        modulesCompleted.countDown();
//                    }
//                }, executorService);
//
//                moduleFutures.add(moduleFuture);
//
//            } catch (Exception e) {
//                System.err.println("모듈 처리 준비 중 오류: " + e.getMessage());
//                e.printStackTrace();
//                modulesCompleted.countDown();
//            }
//        }
//
//        // 모든 모듈 처리가 완료될 때까지 대기
//        modulesCompleted.await();
//
//        System.out.println("모든 모듈 처리 완료");
//    }
//
//    public void insertMethodExample(String moduleName) throws JsonProcessingException, InterruptedException {
//        // 1. 모듈 명으로 모듈 엔티티 조회 (content_type = 'module')
//        ContentsEntity moduleEntity = contentsRepository.findModuleName(moduleName);
//        if (moduleEntity == null) {
//            throw new RuntimeException("모듈 [" + moduleName + "] 을(를) 찾을 수 없습니다.");
//        }
//
//        // 2. 모듈의 contentDetail 파싱
//        JsonNode moduleJson = objectMapper.readTree(moduleEntity.getContentDetail());
//        ArrayNode packageArray = (ArrayNode) moduleJson.get("packages");
//        if (packageArray == null || packageArray.size() == 0) {
//            throw new RuntimeException("모듈 내에 패키지 정보가 없습니다.");
//        }
//
//        List<CompletableFuture<Void>> allFutures = new ArrayList<>();
//        CountDownLatch allTasksCompleted = new CountDownLatch(packageArray.size());
//
//        // 3. 각 패키지 uid에 대해 비동기 처리
//        for (JsonNode packageUidNode : packageArray) {
//            String packageUid = packageUidNode.asText();
//
//            CompletableFuture<Void> packageFuture = CompletableFuture.runAsync(() -> {
//                try {
//                    processPackage(packageUid);
//                } catch (Exception e) {
//                    System.err.println("패키지 [" + packageUid + "] 처리 중 오류 발생: " + e.getMessage());
//                    e.printStackTrace();
//                } finally {
//                    allTasksCompleted.countDown();
//                }
//            }, executorService);
//
//            allFutures.add(packageFuture);
//        }
//
//        // 모든 작업이 완료될 때까지 대기
//        allTasksCompleted.await();
//
//        // 작업 완료 후 스레드 풀 정리
//        // executorService.shutdown(); // 서비스 종료 시 호출하는 것이 좋습니다
//
//        System.out.println("모듈 [" + moduleName + "] 내 모든 클래스 처리 완료");
//    }
//
//    private void processPackage(String packageUid) throws Exception {
//        // 패키지 엔티티 조회
//        ContentsEntity packageEntity = contentsRepository.findPackageByUid(packageUid);
//        if (packageEntity == null) {
//            System.err.println("패키지 uid [" + packageUid + "] 를 찾을 수 없습니다.");
//            return;
//        }
//
//        // 패키지의 contentDetail 파싱
//        JsonNode packageJson = objectMapper.readTree(packageEntity.getContentDetail());
//        ArrayNode classesArray = (ArrayNode) packageJson.get("classes");
//        if (classesArray == null || classesArray.size() == 0) {
//            System.err.println("패키지 uid [" + packageUid + "] 에 클래스 정보가 없습니다.");
//            return;
//        }
//
//        List<CompletableFuture<Void>> classFutures = new ArrayList<>();
//
//        // 각 클래스에 대해 비동기 처리
//        for (JsonNode classUidNode : classesArray) {
//            String classUid = classUidNode.asText();
//
//            CompletableFuture<Void> classFuture = CompletableFuture.runAsync(() -> {
//                try {
//                    processClass(classUid);
//                } catch (Exception e) {
//                    System.err.println("클래스 [" + classUid + "] 처리 중 오류 발생: " + e.getMessage());
//                    e.printStackTrace();
//                }
//            }, executorService);
//
//            classFutures.add(classFuture);
//        }
//
//        // 모든 클래스 처리가 완료될 때까지 대기
//        CompletableFuture.allOf(classFutures.toArray(new CompletableFuture[0])).join();
//    }
//
//    private void processClass(String classUid) throws Exception {
//        // 레이트 리밋 체크 및 대기
//        waitForRateLimit();
//
//        // 클래스 엔티티 조회
//        ContentsEntity classEntity = contentsRepository.findClassByUid(classUid);
//        if (classEntity == null) {
//            System.err.println("클래스 uid [" + classUid + "] 를 찾을 수 없습니다.");
//            return;
//        }
//
//        // 클래스의 contentDetail 파싱
//        String classContentJson = classEntity.getContentDetail();
//
//        try {
//            // 원본 클래스 contentDetail JSON 파싱
//            ObjectNode classContentNode = (ObjectNode) objectMapper.readTree(classContentJson);
//            ArrayNode methodsArray = (ArrayNode) classContentNode.get("methods");
//
//            // methods 배열이 비어있거나 null이면 처리하지 않음
//            if (methodsArray == null || methodsArray.size() == 0) {
//                System.out.println("클래스 [" + classUid + "] 에 메서드 정보가 없습니다.");
//                return;
//            }
//
//            // AI 호출 전 요청 카운터 증가
//            incrementRequestCounter();
//
//            // Groq API 호출 및 재시도 로직
//            String methodExampleJson = callGroqWithRetry(classContentJson);
//            if (methodExampleJson == null) {
//                System.err.println("클래스 [" + classUid + "] AI 응답 실패, 건너뜁니다.");
//                return;
//            }
//
//            // Groq API에서 받은 methodExample JSON 배열 파싱
//            ArrayNode methodExampleArray = parseMethodExampleJson(methodExampleJson);
//            if (methodExampleArray == null) {
//                System.err.println("클래스 [" + classUid + "] JSON 파싱 실패, 건너뜁니다.");
//                return;
//            }
//
//            // 각 메서드에 대해 순서대로 methodExample 값 업데이트
//            updateMethodExamples(methodsArray, methodExampleArray);
//
//            // 업데이트된 JSON을 문자열로 변환하여 DB에 저장
//            String updatedClassContentJson = objectMapper.writeValueAsString(classContentNode);
//            classEntity.setContentDetail(updatedClassContentJson);
//            contentsRepository.save(classEntity);
//            System.out.println("클래스 [" + classUid + "] 업데이트 완료");
//
//        } catch (Exception e) {
//            System.err.println("클래스 [" + classUid + "] 처리 중 오류: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    private String callGroqWithRetry(String classContentJson) {
//        int maxRetries = 3;
//        int retryCount = 0;
//        long retryDelayMs = 1000; // 초기 재시도 지연 시간 (1초)
//
//        while (retryCount < maxRetries) {
//            try {
//                // Groq API 호출
//                return createMethodExample(classContentJson);
//            } catch (Exception e) {
//                retryCount++;
//                System.err.println("AI 호출 실패 (시도 " + retryCount + "/" + maxRetries + "): " + e.getMessage());
//
//                if (retryCount >= maxRetries) {
//                    System.err.println("최대 재시도 횟수 초과: " + e.getMessage());
//                    break;
//                }
//
//                try {
//                    // 지수 백오프 적용
//                    Thread.sleep(retryDelayMs);
//                    retryDelayMs *= 2; // 다음 재시도는 2배 긴 시간 동안 대기
//                } catch (InterruptedException ie) {
//                    Thread.currentThread().interrupt();
//                    break;
//                }
//            }
//        }
//
//        return null;
//    }
//
//    private ArrayNode parseMethodExampleJson(String methodExampleJson) {
//        int maxRetries = 2;
//        for (int attempt = 0; attempt < maxRetries; attempt++) {
//            try {
//                return (ArrayNode) objectMapper.readTree(methodExampleJson);
//            } catch (JsonProcessingException e) {
//                System.err.println("JSON 파싱 오류 (시도 " + (attempt + 1) + "/" + maxRetries + "): " + e.getMessage());
//                if (attempt == maxRetries - 1) {
//                    return null;
//                }
//            }
//        }
//        return null;
//    }
//
//    private void updateMethodExamples(ArrayNode methodsArray, ArrayNode methodExampleArray) {
//        for (int i = 0; i < methodsArray.size(); i++) {
//            ObjectNode methodNode = (ObjectNode) methodsArray.get(i);
//            if (i < methodExampleArray.size()) {
//                JsonNode exampleNode = methodExampleArray.get(i).get("example");
//                if (exampleNode != null) {
//                    methodNode.put("methodExample", exampleNode.asText());
//                }
//            } else {
//                System.out.println("메서드 인덱스 " + i + " 에 대한 예시 데이터가 없습니다.");
//            }
//        }
//    }
//
//    // 레이트 리밋 체크 및 대기 메서드
//    private void waitForRateLimit() throws InterruptedException {
//        requestRateLimiter.acquire(); // 세마포어를 사용하여 동시 요청 수 제한
//
//        synchronized (this) {
//            LocalDateTime now = LocalDateTime.now();
//
//            // 1분이 지났으면 카운터 초기화
//            if (now.isAfter(currentMinuteStart.plusMinutes(1))) {
//                currentMinuteStart = now;
//                requestsInCurrentMinute.set(0);
//                tokensInCurrentMinute.set(0);
//                System.out.println("새로운 분 시작: 레이트 리밋 카운터 초기화");
//            }
//
//            // RPM 제한에 도달했으면 다음 분까지 대기
//            if (requestsInCurrentMinute.get() >= 480) { // 안전 마진으로 RPM 500 대신 480 사용
//                long waitTimeMs = currentMinuteStart.plusMinutes(1).toEpochSecond(java.time.ZoneOffset.UTC) * 1000
//                        - now.toEpochSecond(java.time.ZoneOffset.UTC) * 1000;
//                System.out.println("RPM 제한에 도달, " + waitTimeMs + "ms 대기");
//                Thread.sleep(waitTimeMs + 100); // 안전 마진으로 100ms 추가
//
//                // 대기 후 새 분으로 리셋
//                currentMinuteStart = LocalDateTime.now();
//                requestsInCurrentMinute.set(0);
//                tokensInCurrentMinute.set(0);
//            }
//        }
//    }
//
//    // 요청 카운터 증가 메서드
//    private void incrementRequestCounter() {
//        requestsInCurrentMinute.incrementAndGet();
//        // 추정 토큰 소비량 증가 (요청당 평균 토큰 소비량 추정)
//        tokensInCurrentMinute.addAndGet(250); // 요청당 평균 토큰 수 예상치
//
//        System.out.println("현재 분 요청 수: " + requestsInCurrentMinute.get() +
//                          ", 추정 토큰 수: " + tokensInCurrentMinute.get());
//    }
//
//    public String createMethodExample(String javaClassString){
//        List<MessageDTO> messageDTOList = new ArrayList<>();
//
//        messageDTOList.add(getGroqSystemPrompt(javaClassString));
//        messageDTOList.add(getGroqUserDTOS(javaClassString));
//
//        GroqContentRequestDTO groqContentRequestDTO = new GroqContentRequestDTO();
//        groqContentRequestDTO.setMessages(messageDTOList);
//        groqContentRequestDTO.setModel("qwen-2.5-coder-32b");
//
//        ContentResponseDTO contentResponseDTO = groqAPI.getGroqChat(groqContentRequestDTO, groqAccessToken, "application/json");
//        return contentResponseDTO.getContent();
//    }
//
//    @NotNull
//    private static MessageDTO getGroqUserDTOS(String methods) {
//        MessageDTO userMessageDTO = new MessageDTO();
//        userMessageDTO.setRole("user");
//
//        String userMessage = "아래 조건에 맞는 JSON 배열 데이터를 생성해 주세요. " +
//                "출력은 반드시 오직 JSON 배열만 포함해야 하며, " +
//                "추가적인 설명이나 예시 문구(예: '아래는 요구 사항을 충족하는 JSON 형식의 배열입니다:' 등)는 절대로 포함하지 않아야 합니다. " +
//                "입력된 메서드 목록: %s.\n" +
//                "3. 각 JSON 객체는 다음 필드들을 포함해야 합니다:\\n\" +\n" +
//                "   - example: 문자열 타입이며, 메서드 목록에서 조회된 메서드 명 순서대로 받은 메서드의 대표적인 사용 예시를 코드 형태로 제공되어야 합니다.\" ";
//
//
//        String userPrompt = String.format(userMessage, methods);
//
//        userMessageDTO.setContent(userPrompt);
//        return userMessageDTO;
//    }

//    @NotNull
//    private static MessageDTO getGroqSystemPrompt(String javaClassString) {
//        MessageDTO systemMessageDTO = new MessageDTO();
//        systemMessageDTO.setRole("system");
//
//        String systemMessage = "당신은 Java Class or Interface의 Method의 대표적인 사용 예시 데이터를 생성하는 역할을 수행합니다. " +
//                "아래 조건을 모두 만족하는 JSON 데이터를 생성해 주세요. " +
//                "1. 출력 결과는 반드시 올바른 JSON 형식의 배열이어야 하며, " +
//                "부가 설명이나 추가 문구(예: '아래는 요구 사항을 충족하는 JSON 형식의 배열입니다:' 등)는 전혀 포함하지 않아야 합니다. " +
//                "2. 최종 출력은 반드시 한국어로 번역되어야 합니다." +
//                "입력된 클래스 및 메서드 목록: %s. " +
//                "3. 각 JSON 객체는 다음 필드들을 포함해야 합니다:\n" +
//                "   - example: 문자열 타입이며, 메서드 목록에서 조회된 메서드 명 순서대로 받은 메서드의 대표적인 사용 예시를 코드 형태로 제공되어야 합니다." +
//                "예시:\n" +
//                "{\n" +
//                "    \"example\": \"String str = \\\"Hello\\\";\\nint length = str.length(); // 결과: 5\",\n" +
//                "},\n" +
//                "{\n" +
//                "    \"example\": \"String str = \\\"Hello\\\";\\nchar ch = str.charAt(0); // 결과: 'H'\",\n" +
//                "}";
//
//        String systemPrompt = String.format(systemMessage, javaClassString);
//        systemMessageDTO.setContent(systemPrompt);
//
//        return systemMessageDTO;
//    }
}
