package com.project.moduleservicecontent.controller;

import com.project.moduleservicecontent.dto.PagedResponse;
import com.project.moduleservicecontent.dto.TechBlogResponseDTO;
import com.project.moduleservicecontent.entity.ContentsEntity;
import com.project.moduleservicecontent.service.TechBlogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/techblog")
@RequiredArgsConstructor
@Slf4j
public class TechBlogController {

    private final TechBlogService techBlogService;

    // 테크블로그 전체
    // http://127.0.0.1:5004/api/techblog/all
    @GetMapping("/all")
    public ResponseEntity<List<ContentsEntity>> getTechBlog() {
        List<ContentsEntity> blogs = techBlogService.getAllTechBlog();
//        System.out.println(blogs.size());
        return ResponseEntity.ok(blogs);
    }

    // provider별 출력
    // ex) http://127.0.0.1:5004/api/techblog/provider/toss
    @GetMapping("/provider/{provider}")
    public ResponseEntity<List<ContentsEntity>> getTechBlogbyProvider(@PathVariable String provider) {
        List<ContentsEntity> blogs = techBlogService.getTechBlogByProvider(provider);
//        System.out.println(blogs.size());
        return ResponseEntity.ok(blogs);
    }

    @GetMapping
    public ResponseEntity<PagedResponse<TechBlogResponseDTO>> getTechBlogs(
            @RequestParam(required = false) String provider,
            @RequestParam(defaultValue = "latest") String sort,
            @PageableDefault(page = 0, size = 10) Pageable pageable,
            @RequestHeader(value = "X-User-ID", required = false) Long userId) {
        System.out.println("provider = " + provider);
        System.out.println("userId = " + userId);
        Pageable unSortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.unsorted());
        return ResponseEntity.ok(techBlogService.getTechBlogs(provider, sort, unSortedPageable, userId));
    }

    @PostMapping("/view/{id}")
    public ResponseEntity<Void> increaseViewCount(@PathVariable Long id) {
        techBlogService.increaseViewCount(id);
        return ResponseEntity.ok().build(); // 성공만 내려줌
    }

    @GetMapping("/popular")
    public ResponseEntity<PagedResponse<TechBlogResponseDTO>> getPopularTechBlogs(
            @RequestHeader(value = "X-User-ID", required = false) Long userId) {

        // 정렬 없는 Pageable (Hibernate에게 ORDER BY 전달 X)
        Pageable pageable = PageRequest.of(0, 3, Sort.unsorted());

        // 서비스 레이어는 sort 파라미터 통해 'bookmarks' 정렬로 native query에서 처리
        return ResponseEntity.ok(techBlogService.getTechBlogs(null, "bookmarks", pageable, userId));
    }

}
