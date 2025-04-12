package com.project.moduleservicedatacollection.component;

import com.project.moduleservicedatacollection.dto.TechBlog.ArticleDetails;
import com.project.moduleservicedatacollection.service.TechBlog.TechBlogService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TechBlogManager {
    private final Map<String, TechBlogService> blogServices = new HashMap<>();

    // 생성자에서 각 서비스의 getSupportedBlogTypes() 메서드를 호출하여, 지원하는 모든 blogType 키를 등록합니다.
    public TechBlogManager(List<TechBlogService> services) {
        for (TechBlogService service : services) {
            for (String key : service.getSupportedBlogTypes()) {
                blogServices.put(key.toLowerCase(), service);
            }
        }
        System.out.println("지원하는 블로그: " + blogServices.keySet());
    }

    // blogType에 해당하는 서비스의 fetchArticles() 호출
    public List<ArticleDetails> fetchArticles(String blogType) {
        TechBlogService service = blogServices.get(blogType.toLowerCase());
        if (service == null) {
            throw new IllegalArgumentException("No such service: " + blogType);
        }
        return service.fetchArticles(blogType);
    }
}