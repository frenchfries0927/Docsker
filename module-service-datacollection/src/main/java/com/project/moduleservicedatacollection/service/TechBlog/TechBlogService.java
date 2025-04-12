package com.project.moduleservicedatacollection.service.TechBlog;

import com.project.moduleservicedatacollection.dto.TechBlog.ArticleDetails;

import java.util.List;

public interface TechBlogService {
    // blogType에 따라 기사 목록을 크롤링하는 메서드
    List<ArticleDetails> fetchArticles(String blogType);

    // 해당 서비스가 지원하는 blogType 목록을 반환 (예: devocean, danawa 등)
    default List<String> getSupportedBlogTypes() {
        return List.of();
    }
}
