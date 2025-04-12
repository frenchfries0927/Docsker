package com.project.moduleserviceuser.dto.bookmark;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ClassBookmarkResponse implements BookmarkResponse{
    private Long contentId;
    private String title;
    private String type;
    private String module;
    private String packageName;
    private String createAt;
}
