package com.project.moduleserviceuser.dto.bookmark;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


//User Entity에 저장할 북마크 목록 DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentBookmarkDTO {
    private Long contentId;
    private String createAt;
}
