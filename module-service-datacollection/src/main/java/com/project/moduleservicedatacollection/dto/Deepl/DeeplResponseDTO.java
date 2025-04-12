package com.project.moduleservicedatacollection.dto.Deepl;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
//
@Data
@AllArgsConstructor
public class DeeplResponseDTO {
    @JsonProperty("translations")
    private List<DeeplResponseTextDTO> translations;
}
