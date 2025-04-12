package com.project.moduleserviceuser.dto.bookmark;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Getter
@Builder
public class TechBlogContentResponseDTO implements ContentResponseDTO{
    private Long contentId;
    private String title;
    private String type; // 항상 "TechBlog"
    private String blogProvider;
    private String summary;
    private String publishedDate;
}
