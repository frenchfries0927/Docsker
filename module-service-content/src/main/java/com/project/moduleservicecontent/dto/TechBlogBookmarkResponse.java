package com.project.moduleservicecontent.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TechBlogBookmarkResponse implements BookmarkResponse{
    private Long contentId;
    private String title;
    private String type;
    private String blogProvider;
    private String summary;
    private String publishedDate;
    private String link;
}
