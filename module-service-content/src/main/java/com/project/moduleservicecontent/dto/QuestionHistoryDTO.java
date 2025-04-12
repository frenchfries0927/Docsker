package com.project.moduleservicecontent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionHistoryDTO {
    private Long id;
    private String date;
    private String question;
    private String userAnswer;
    private String llmAnswer;
    private boolean isToday;
    private boolean isBookmarked;
} 