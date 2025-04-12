package com.project.moduleservicedatacollection.service.Deepl;

import com.project.moduleservicedatacollection.dto.Deepl.DeeplDTO;
import com.project.moduleservicedatacollection.dto.Deepl.DeeplResponseDTO;
import com.project.moduleservicedatacollection.dto.Deepl.DeeplResponseTextDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeeplAPIService {

    private final DeeplAPI deeplAPI;

    @Value("${deepl.api.key}")
    private String apiKey;

    public List<String> translateText(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> translatedTexts = new ArrayList<>();
        int batchSize = 50; // 한 번에 처리할 문장 개수
        String authorizationHeader = "DeepL-Auth-Key " + apiKey;

        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, end);
            DeeplDTO request = new DeeplDTO(batch, "KO");

            try {
                DeeplResponseDTO response = deeplAPI.translate(request, authorizationHeader);

                if (response != null && response.getTranslations() != null) {
                    for (DeeplResponseTextDTO textDTO : response.getTranslations()) {
                        translatedTexts.add(textDTO.getText());
                    }
                } else {
                    log.warn("DeepL API 응답이 null이거나 번역된 데이터가 없습니다.");
                }

                // API 호출 후 딜레이 추가
                Thread.sleep(2000); // 1초 대기

            } catch (Exception e) {
                log.error("DeepL API 호출 중 오류 발생: {}", e.getMessage(), e);

                // 429 오류 발생 시 3초 대기 후 재시도
                if (e.getMessage().contains("429 Too Many Requests")) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        return translatedTexts;
    }

}
