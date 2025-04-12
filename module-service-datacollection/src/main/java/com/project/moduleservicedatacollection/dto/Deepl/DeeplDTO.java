package com.project.moduleservicedatacollection.dto.Deepl;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DeeplDTO {
    private List<String> text;   // 번역할 텍스트 리스트
    @JsonProperty("target_lang")
    private String targetLang;  // 번역할 언어
}