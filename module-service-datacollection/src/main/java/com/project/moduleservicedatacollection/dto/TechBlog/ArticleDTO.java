package com.project.moduleservicedatacollection.dto.TechBlog;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArticleDTO {
    private String title;  // 게시글 제목
    private String url;    // 게시글 URL
    private String summary;  // 게시글 요약
    private List<String> tags;  // 게시글 태그들
    private String publishedAt;  // 게시일
}
