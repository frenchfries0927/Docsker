package com.project.moduleservicedatacollection.dto.Deepl;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeeplResponseTextDTO {
    @JsonProperty("detected_source_language")
    private String detectedSourceLanguage;

    @JsonProperty("text")
    private String text;
}
