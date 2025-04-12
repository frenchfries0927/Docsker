package com.project.moduleservicecontent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAnswerDTO {
    private Long userId;
    private String answer;
    private String llmAnswer;
    private Integer score;
} 