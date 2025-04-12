package com.project.moduleservicedatacollection.service.TechBlog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.project.moduleservicedatacollection.dto.TodayQuestion.ContentResponseDTO;
import com.project.moduleservicedatacollection.dto.TodayQuestion.HFContentRequestDTO;
import com.project.moduleservicedatacollection.dto.TodayQuestion.MessageDTO;
import com.project.moduleservicedatacollection.entity.ContentsEntity;
import com.project.moduleservicedatacollection.repository.ContentsRepository;
import com.project.moduleservicedatacollection.service.AI.HuggingFaceAPI;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GenerateTagService {

    private final HuggingFaceAPI huggingFaceAPI;
    private final ContentsRepository contentsRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void generateTagsForProvider(String provider) {
        List<ContentsEntity> allContents = contentsRepository.findAll();

        for (ContentsEntity content : allContents) {
            try {
                JsonNode detailNode = objectMapper.readTree(content.getContentDetail());

                String contentProvider = detailNode.path("provider").asText(null);
                String link = detailNode.path("link").asText(null);

                if (contentProvider == null || !contentProvider.equalsIgnoreCase(provider)) continue;
                if (link == null || link.isBlank()) continue;

                String blogText = extractContentFromUrl(link);
                if (blogText.isBlank()) continue;

                List<String> tags = requestTagsFromHuggingFace(blogText);
                if (!tags.isEmpty()) {
                    // contentDetail에 tags 필드 추가
                    ((ObjectNode) detailNode).putPOJO("tags", tags);
                    System.out.println("tags = " + tags);
                    // 다시 JSON 문자열로 저장
                    String updatedDetail = objectMapper.writeValueAsString(detailNode);
                    content.setContentDetail(updatedDetail);

//                    contentsRepository.save(content);
                }
            } catch (Exception e) {
                System.err.println("[" + content.getId() + "] 처리 중 오류: " + e.getMessage());
            }
        }
    }

    public String extractContentFromUrl(String url) {
        try {
            return Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .get()
                    .select("article, div.post-content, div.blog-body, main")
                    .text();
        } catch (IOException e) {
            throw new RuntimeException("크롤링 실패: " + url, e);
        }
    }

    private List<String> requestTagsFromHuggingFace(String blogText) {
        MessageDTO system = new MessageDTO("system", "기술 블로그 내용을 분석하여 관련 태그를 추출하세요. 쉼표로 구분된 태그만 반환하세요.");
        MessageDTO user = new MessageDTO("user", blogText);

        HFContentRequestDTO request = new HFContentRequestDTO();
        request.setModel("gpt-3.5-turbo");
        request.setMessages(List.of(system, user));

        ContentResponseDTO response = huggingFaceAPI.getHFChat(request, "application/json");

        return Arrays.stream(response.getContent().split(","))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .toList();
    }
}
