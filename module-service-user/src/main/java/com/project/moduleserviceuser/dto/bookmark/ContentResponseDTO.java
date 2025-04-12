package com.project.moduleserviceuser.dto.bookmark;

//Feign을 통해 받아올 컨텐츠 DTO
public interface ContentResponseDTO {
    Long getContentId();
    String getTitle();
    String getType();
}