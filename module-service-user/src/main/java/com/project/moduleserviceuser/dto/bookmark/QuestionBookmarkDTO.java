package com.project.moduleserviceuser.dto.bookmark;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionBookmarkDTO {
    private Long questionId;
    private String createAt;
}
