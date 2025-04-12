package com.project.moduleservicecontent.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassResponseDTO {
    private Long id;
    private String uid;
    private JsonNode contentDetail; // JsonNode 그대로 넘김 (name, description 등 포함)
    private Integer views;
    private boolean bookmarked;
}