package com.project.moduleservicecontent.dto;

import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class ClassBookmarkResponse implements BookmarkResponse{
    private Long contentId;
    private String title;
    private String type;
    private String module;
    private String packageName;
}
