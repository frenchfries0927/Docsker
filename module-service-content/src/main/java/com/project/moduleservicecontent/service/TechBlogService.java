package com.project.moduleservicecontent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.moduleservicecontent.dto.PagedResponse;
import com.project.moduleservicecontent.dto.TechBlogResponseDTO;
import com.project.moduleservicecontent.entity.ContentsEntity;
import com.project.moduleservicecontent.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;


import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TechBlogService {
    private final ContentRepository contentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<ContentsEntity> getAllTechBlog(){
        return contentRepository.findAllTechBlog();
    }

    public List<ContentsEntity> getTechBlogByProvider(String provider){
        return contentRepository.findbyTechBlogprovider(provider);
    }

    @Transactional
    public void increaseViewCount(Long id) {
        contentRepository.incrementViews(id);
    }


    public PagedResponse<TechBlogResponseDTO> getTechBlogs(String provider, String sort, Pageable pageable, Long userId) {
        Page<ContentsEntity> blogsPage = contentRepository.findTechBlogs(provider, sort, pageable);
        List<TechBlogResponseDTO> dtoList = blogsPage.stream()
                .map(content -> {
                    boolean isBookmarked = false;
                    if (userId != null) {
                        isBookmarked = content.getBookmarkUsersList().contains(userId);
                        System.out.println("isBookmarked = " + isBookmarked);
                    }

                    List<String> tags = content.getJsonFieldAsObject("tags", new TypeReference<List<String>>() {});
                    if (tags == null) tags = List.of();
                    System.out.println("tags = " + tags);
                    return TechBlogResponseDTO.builder()
                            .id(content.getId())
                            .title(content.getJsonField("title"))
                            .link(content.getJsonField("link"))
                            .summary(content.getJsonField("content"))
                            .provider(content.getJsonField("provider"))
                            .publishDate(content.getJsonField("publishDate"))
                            .views(content.getViews())
                            .bookmarkCount(content.getBookmarkUsersList().size())
                            .isBookmarked(isBookmarked)
                            .tags(tags)
                            .build();
                })
                .collect(Collectors.toList());

        return PagedResponse.<TechBlogResponseDTO>builder()
                .content(dtoList)
                .page(blogsPage.getNumber())
                .size(blogsPage.getSize())
                .totalPages(blogsPage.getTotalPages())
                .totalElements(blogsPage.getTotalElements())
                .last(blogsPage.isLast())
                .build();
    }

}
