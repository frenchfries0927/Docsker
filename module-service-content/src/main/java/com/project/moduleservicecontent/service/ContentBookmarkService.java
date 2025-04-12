package com.project.moduleservicecontent.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.moduleservicecontent.dto.BookmarkResponse;
import com.project.moduleservicecontent.dto.ClassBookmarkResponse;
import com.project.moduleservicecontent.dto.TechBlogBookmarkResponse;
import com.project.moduleservicecontent.entity.ContentsEntity;
import com.project.moduleservicecontent.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentBookmarkService {

    private final ContentRepository contentRepository;
    private final ObjectMapper objectMapper;

    // ✅ Map 형태로 바로 반환
    public List<Map<String, Object>> getBookmarkedContents(List<Long> contentIds) {
        List<ContentsEntity> contents = contentRepository.findAllById(contentIds);

        List<BookmarkResponse> responses = contents.stream()
                .map(this::mapToBookmarkResponse)
                .collect(Collectors.toList());

        // BookmarkResponse → Map으로 변환
        return responses.stream()
                .map(response -> objectMapper.convertValue(response, new TypeReference<Map<String, Object>>() {}))
                .collect(Collectors.toList());
    }

    private BookmarkResponse mapToBookmarkResponse(ContentsEntity content) {
        String type = content.getContentType();

        if ("class".equals(type)) {
            return buildClassBookmarkResponse(content);
        } else if ("TechBlog".equals(type)) {
            return buildTechBlogBookmarkResponse(content);
        } else {
            throw new IllegalArgumentException("Unknown content type: " + type);
        }
    }

    private ClassBookmarkResponse buildClassBookmarkResponse(ContentsEntity content) {
        String packageUid = content.getJsonField("packageId");
        String packageName = null;
        String moduleName = null;

        if (packageUid != null) {
            Optional<ContentsEntity> packageContent = contentRepository.findByUid(packageUid);
            packageName = packageContent.map(p -> p.getJsonField("name")).orElse(null);

            String moduleUid = packageContent.map(p -> p.getJsonField("moduleId")).orElse(null);
            if (moduleUid != null) {
                Optional<ContentsEntity> moduleContent = contentRepository.findByUid(moduleUid);
                moduleName = moduleContent.map(m -> m.getJsonField("name")).orElse(null);
            }
        }

        return ClassBookmarkResponse.builder()
                .contentId(content.getId())
                .title(content.getJsonField("name"))
                .type(content.getContentType())
                .module(moduleName)
                .packageName(packageName)
                .build();
    }

    private TechBlogBookmarkResponse buildTechBlogBookmarkResponse(ContentsEntity content) {
        return TechBlogBookmarkResponse.builder()
                .contentId(content.getId())
                .title(content.getJsonField("title"))
                .type(content.getContentType())
                .blogProvider(content.getJsonField("provider"))
                .summary(content.getJsonField("content"))
                .publishedDate(content.getJsonField("publishDate"))
                .link(content.getJsonField("link"))
                .build();
    }

    //kafka 관련 서비스
    /**
     * Content Bookmark 추가 시 content table userId 추가
     * */
    public void addBookmarkUser(Long contentId, Long userId) {
        ContentsEntity content = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("콘텐츠 없음"));

        // 기존 유저 리스트 불러오기
        List<Long> bookmarkUsers = content.getBookmarkUsersList();

        // 이미 포함되어 있는지 확인
        if (!bookmarkUsers.contains(userId)) {
            bookmarkUsers.add(userId);
            content.setBookmarkUsersList(bookmarkUsers); // 변경된 부분
            contentRepository.save(content);
        }
    }

    /**
     * 콘텐츠 북마크 삭제
     */
    public void removeBookmarkUser(Long contentId, Long userId) {
        ContentsEntity content = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("콘텐츠 없음"));

        List<Long> bookmarkUsers = content.getBookmarkUsersList();

        if (bookmarkUsers.contains(userId)) {
            bookmarkUsers.remove(userId);
            content.setBookmarkUsersList(bookmarkUsers);
            contentRepository.save(content);
        }
    }
}
