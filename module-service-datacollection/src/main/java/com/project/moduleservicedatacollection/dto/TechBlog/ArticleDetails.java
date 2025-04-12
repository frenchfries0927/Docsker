package com.project.moduleservicedatacollection.dto.TechBlog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class ArticleDetails {
    private String provider;
    private String title;
    private String link;
    private String content;
    private ArrayList<String> tags;
    private LocalDate publishDate;
}
