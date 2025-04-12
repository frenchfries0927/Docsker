package com.project.moduleservicedatacollection.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class GroqHttpUtil {
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Groq API 파일 업로드 메서드
     *
     * 실제 파일을 multipart/form-data 형식으로 업로드하고,
     * 응답 JSON에서 파일 ID를 추출하여 반환합니다.
     *
     * 예: 업로드 후 "file_01jh6x76wtemjr74t1fh0faj5t"와 같은 ID 반환
     */
    public static String uploadFile(String apiKey, String filePath, String purpose) throws Exception {
        String url = "https://api.groq.com/openai/v1/files";
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost uploadFile = new HttpPost(url);
            uploadFile.setHeader("Authorization", apiKey);

            // multipart/form-data 빌더 생성
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addTextBody("purpose", purpose);
            File file = new File(filePath);
            builder.addBinaryBody("file", file, ContentType.APPLICATION_OCTET_STREAM, file.getName());

            HttpEntity multipart = builder.build();
            uploadFile.setEntity(multipart);

            try (CloseableHttpResponse response = httpClient.execute(uploadFile)) {
                HttpEntity responseEntity = response.getEntity();
                String responseString = EntityUtils.toString(responseEntity, "UTF-8");
                JsonNode jsonNode = mapper.readTree(responseString);
                return jsonNode.get("id").asText();
            }
        }
    }

    /**
     * POST 요청을 보내고 JSON 응답을 파싱하여 반환하는 메서드.
     */
    public static JsonNode postJson(String url, String apiKey, String jsonBody) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);
            post.setHeader("Authorization", apiKey);
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpClient.execute(post)) {
                HttpEntity responseEntity = response.getEntity();
                String responseString = EntityUtils.toString(responseEntity, "UTF-8");
                return mapper.readTree(responseString);
            }
        }
    }

    /**
     * GET 요청을 보내고 JSON 응답을 파싱하여 반환하는 메서드.
     */
    public static JsonNode getJson(String url, String apiKey) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(url);
            get.setHeader("Authorization", apiKey);

            try (CloseableHttpResponse response = httpClient.execute(get)) {
                HttpEntity responseEntity = response.getEntity();
                String responseString = EntityUtils.toString(responseEntity, "UTF-8");
                return mapper.readTree(responseString);
            }
        }
    }

    /**
     * GET 요청으로 파일을 다운로드하여 지정한 경로에 저장하는 메서드.
     */
    public static void downloadFile(String apiKey, String url, String outputFilePath) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(url);
            get.setHeader("Authorization", apiKey);

            try (CloseableHttpResponse response = httpClient.execute(get)) {
                InputStream inputStream = response.getEntity().getContent();
                Files.copy(inputStream, Paths.get(outputFilePath), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /**
     * Batch Job 생성을 위한 메서드.
     * Groq API 문서에 따라 input_file_id, endpoint, completion_window 정보를 포함한 요청을 전송합니다.
     */
    public static String createBatchJob(String apiKey, String inputFileId, String endpoint, String completionWindow) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode data = mapper.createObjectNode();
        data.put("input_file_id", inputFileId);
        data.put("endpoint", endpoint);
        data.put("completion_window", completionWindow);
        JsonNode response = postJson("https://api.groq.com/openai/v1/batches", apiKey, data.toString());
        System.out.println("Batch Job Response: " + response);
        // 배치 작업 생성 후 상태가 "validating"일 수 있으므로, id만 반환
        JsonNode idNode = response.get("id");
        if (idNode == null || idNode.asText().isEmpty()) {
            throw new RuntimeException("Batch Job 생성 실패: " + (response.get("errors") != null ? response.get("errors").asText() : "No ID returned"));
        }
        return idNode.asText();
    }

}
