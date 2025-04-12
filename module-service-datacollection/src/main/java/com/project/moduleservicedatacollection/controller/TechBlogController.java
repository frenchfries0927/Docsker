package com.project.moduleservicedatacollection.controller;

import com.project.moduleservicedatacollection.component.CrawledContentManager;
//import com.project.moduleservicedatacollection.service.TechBlog.Test.TechBlogService1;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TechBlogController {
    private final CrawledContentManager crawledContentManager;

    @PostMapping("/{blogType}")
    public ResponseEntity<String> crawlAndSave(@PathVariable String blogType) {
        try {
            System.out.println("OK");
            crawledContentManager.crawlAndSave(blogType);
            return ResponseEntity.ok("크롤링 및 저장 성공: " + blogType);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("크롤링 실패: " + e.getMessage());
        }
    }


}
