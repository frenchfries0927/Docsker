package com.project.moduleserviceuser.dto.bookmark;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class QuestionBookmarkResponse {
    private Long questionId;
    private String question;
    private String answer;
    private String publishedAt;
}
