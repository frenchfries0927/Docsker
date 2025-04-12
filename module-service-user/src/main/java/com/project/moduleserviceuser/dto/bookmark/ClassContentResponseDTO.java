package com.project.moduleserviceuser.dto.bookmark;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Getter
@Builder
public class ClassContentResponseDTO implements ContentResponseDTO{
    private Long contentId;
    private String title;
    private String type; // 항상 "Doc" or "Class"
    private String module;
    private String packageName;
}
