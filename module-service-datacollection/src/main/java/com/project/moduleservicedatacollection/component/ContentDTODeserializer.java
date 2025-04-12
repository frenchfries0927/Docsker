package com.project.moduleservicedatacollection.component;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.project.moduleservicedatacollection.dto.TodayQuestion.ContentResponseDTO;

import java.io.IOException;

public class ContentDTODeserializer extends JsonDeserializer<ContentResponseDTO> {
    @Override
    public ContentResponseDTO deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        JsonNode rootNode = p.getCodec().readTree(p);

        // choices 배열의 첫 번째 message 의 content 값을 추출
        JsonNode contentNode = rootNode.path("choices").get(0).path("message").path("content");
        String content = contentNode.asText();

        // <think> 태그 내부 내용 제거 (deepseek 모델은 <think> 태그를 제거해줘야 함)
        content = content.replaceAll("(?s)<think>.*?</think>", "").trim();
        return new ContentResponseDTO(content);
    }
}
