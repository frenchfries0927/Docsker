package com.project.moduleserviceuser.dto.bookmark;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
@Builder
@Getter
public class TechBlogBookmarkResponse implements BookmarkResponse {
    private Long contentId;
    private String title;
    private String type;
    private String blogProvider;
    private String summary;
    private String link;
    private String publishedDate;
    private String createAt;
}
