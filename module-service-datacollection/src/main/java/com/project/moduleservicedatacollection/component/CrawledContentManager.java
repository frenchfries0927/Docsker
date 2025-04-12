package com.project.moduleservicedatacollection.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.moduleservicedatacollection.dto.TechBlog.ArticleDetails;
import com.project.moduleservicedatacollection.entity.ContentsEntity;
import com.project.moduleservicedatacollection.repository.ContentsRepository;
import com.project.moduleservicedatacollection.repository.CrawledContentRepository;
import com.project.moduleservicedatacollection.entity.CrawledContent;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CrawledContentManager {
//    private final CrawledContentRepository crawledContentRepository;
    private final ContentsRepository contentsRepository;
    private final ObjectMapper objectMapper;
    private final TechBlogManager techBlogManager;

    public CrawledContentManager(ContentsRepository repository, ObjectMapper objectMapper, TechBlogManager techBlogManager) {
        this.contentsRepository = repository;
        this.objectMapper = objectMapper;
        this.techBlogManager = techBlogManager;
    }

    @Transactional
    public void crawlAndSave(String blogType) {
        System.out.println("OK");
        List<ArticleDetails> articles = techBlogManager.fetchArticles(blogType);
        for (ArticleDetails article : articles) {
            try {
                // ✅ 글 하나씩 JSON 변환
                String jsonDetails = objectMapper.writeValueAsString(article);

                // ✅ 개별 글을 DB에 저장
//                CrawledContent content = CrawledContent.builder()
//                        .contentType("TechBlog")
//                        .contentDetails(jsonDetails) // ✅ 글 하나씩 저장
//                        .build();
                ContentsEntity content = ContentsEntity.builder()
                        .contentType("TechBlog")
                        .contentDetail(jsonDetails) // ✅ 글 하나씩 저장
                        .views(0)
                        .createdAt(LocalDateTime.now())
                        .build();

                contentsRepository.save(content);
//                crawledContentRepository.save(content);
                System.out.println("저장 완료: " + article.getTitle());

            } catch (JsonProcessingException e) {
                System.err.println("JSON 변환 오류: " + article.getTitle());
                e.printStackTrace();
            }
        }
    }

//    public List<CrawledContent> getCrawledDataByType(String blogType) {
//        return crawledContentRepository.findByContentType(blogType);
//    }
}