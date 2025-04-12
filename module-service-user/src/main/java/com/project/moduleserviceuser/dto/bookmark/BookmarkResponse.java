package com.project.moduleserviceuser.dto.bookmark;

//client로 내려줄 DTO
public interface BookmarkResponse {
    Long getContentId();
    String getTitle();
    String getType();
}
