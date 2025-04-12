package com.project.moduleserviceuser.service;

import com.project.moduleserviceuser.dto.bookmark.ContentResponseDTO;
import net.minidev.json.JSONUtil;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "content")
public interface ContentServiceClient {

    @PostMapping("/contents/bookmarks/content")
    List<Map<String, Object>> getBookmarkedContents(@RequestParam List<Long> contentIds);

    @PostMapping("/contents/bookmarks/questions")
    List<Map<String, Object>> getBookmarkedQuestions(@RequestParam List<Long> questionIds, @RequestParam Long userId);
}
