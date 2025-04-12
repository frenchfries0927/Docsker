package com.project.moduleservicecontent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TechBlogResponseDTO {
    private Long id;
    private String title;
    private String link;
    private String summary;
    private String provider;
    private String publishDate;
    private int views;
    private int bookmarkCount;
    @JsonProperty("isBookmarked")
    private boolean isBookmarked;
    private List<String> tags;
}