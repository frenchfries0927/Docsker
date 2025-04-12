package com.project.moduleservicedatacollection.dto.TechBlog;

import com.project.moduleservicedatacollection.service.TechBlog.GenerateTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/techblog/tag")
public class TagExtractionController {
    private final GenerateTagService generateTagService;

    @PostMapping("/{provider}")
    public ResponseEntity<String> generateTagsByProvider(@PathVariable String provider) {
        generateTagService.generateTagsForProvider(provider);
        return ResponseEntity.ok("태그 생성 완료 for " + provider);
    }
}
