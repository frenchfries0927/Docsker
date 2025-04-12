package com.project.moduleservicecontent.dto;

import lombok.*;

@Getter
@Builder
@ToString
public class BookmarkContentResponseDTO {
    private Long id;
    private String title;
    private String type; // "Doc" or "Blog"
    private String module; // Doc 타입일 때
    private String packageName; // Doc 타입일 때
    private String blogProvider; // Blog 타입일 때
}
