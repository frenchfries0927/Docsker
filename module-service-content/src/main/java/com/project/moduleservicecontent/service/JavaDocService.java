package com.project.moduleservicecontent.service;

import com.project.moduleservicecontent.dto.ClassResponseDTO;
import com.project.moduleservicecontent.entity.ContentsEntity;
import com.project.moduleservicecontent.repository.ContentRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class JavaDocService {

    private final ContentRepository contentRepository;
    private final ObjectMapper objectMapper;

    /**
     * 최신 자바 버전 정보 조회
     */
    public ContentsEntity getLatestVersion() {
        return contentRepository.findLatestVersion();
    }

    // 모든 데이터 조회
    public List<ContentsEntity> getAllContentType(String contentType) {
        return contentRepository.findByContentType(contentType);
    }

    /**
     * 특정 UID로 버전 조회
     */
    public ContentsEntity getVersionByUid(String uid) {
        return contentRepository.findByContentTypeAndUid("version", uid).orElse(null);
    }

    /**
     * 특정 버전에 속한 모든 모듈 조회
     */
    public List<ContentsEntity> getModulesByVersionUid(String versionUid) {
        return contentRepository.findModulesByVersionUid(versionUid);
    }

    /**
     * 특정 UID로 모듈 조회
     */
    public ContentsEntity getModuleByUid(String uid) {
        return contentRepository.findByContentTypeAndUid("module", uid).orElse(null);
    }

    /**
     * 특정 모듈에 속한 모든 패키지 조회
     */
    public List<ContentsEntity> getPackagesByModuleUid(String moduleUid) {
        return contentRepository.findPackagesByModuleUid(moduleUid);
    }

    /**
     * 특정 UID로 패키지 조회
     */
    public ContentsEntity getPackageByUid(String uid) {
        return contentRepository.findByContentTypeAndUid("package", uid).orElse(null);
    }

    /**
     * 특정 패키지에 속한 모든 클래스 조회
     */
    public List<ContentsEntity> getClassesByPackageUid(String packageUid) {
        return contentRepository.findClassesByPackageUid(packageUid);
    }

    /**
     * 특정 UID로 클래스 조회
     */
    public ContentsEntity getClassByUid(String uid) {
        return contentRepository.findByContentTypeAndUid("class", uid).orElse(null);
    }

    /**
     * 이름으로 클래스 검색
     */
    public List<ContentsEntity> searchClassesByName(String name) {
        return contentRepository.searchClassesByName(name);
    }

    /**
     * 정확한 이름으로 클래스 찾기
     */
    public ContentsEntity findClassByExactName(String name) {
        return contentRepository.findClassByExactName(name).orElse(null);
    }

    /**
     * 이름으로 패키지 검색
     */
    public List<ContentsEntity> searchPackagesByName(String name) {
        return contentRepository.searchPackagesByName(name);
    }

    /**
     * 타입으로 클래스 조회 (Classes, Interfaces, Enums 등)
     */
    public List<ContentsEntity> getClassesByType(String type) {
        return contentRepository.findClassesByType(type);
    }

    /**
     * 클래스 계층 구조 조회
     */
    public Map<String, ContentsEntity> getClassHierarchy(String classUid) {
        List<ContentsEntity> hierarchyList = contentRepository.findClassHierarchy(classUid);
        Map<String, ContentsEntity> hierarchyMap = new HashMap<>();

        for (ContentsEntity content : hierarchyList) {
            hierarchyMap.put(content.getContentType(), content);
        }

        return hierarchyMap;
    }

    /**
     * JSON 문자열에서 특정 필드 추출
     */
    public String extractFieldFromJson(String json, String fieldName) {
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.has(fieldName)) {
                return node.get(fieldName).asText();
            }
            return null;
        } catch (JsonProcessingException e) {
            log.error("JSON 파싱 오류", e);
            return null;
        }
    }

    /**
     * 모든 필드 추출해서 Map으로 변환
     */
    public Map<String, Object> jsonToMap(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            log.error("JSON 맵 변환 오류", e);
            return new HashMap<>();
        }
    }

    @Transactional
    public void increaseViewCount(Long id) {
        contentRepository.incrementViews(id);
    }

    // 검색을 위해 전체 List<class>에 북마크 boolean 추가
    public List<ClassResponseDTO> getClassesTest(Long userId) {
        List<ContentsEntity> classes = contentRepository.findByContentType("class");

        return classes.stream().map(cls -> {
            boolean isBookmarked = userId != null && cls.getBookmarkUsersList().contains(userId);
            JsonNode contentDetailNode = null;
            try {
                contentDetailNode = objectMapper.readTree(cls.getContentDetail());
            } catch (Exception e) {
                log.error("Failed to parse contentDetail", e);
            }

            return ClassResponseDTO.builder()
                    .id(cls.getId())
                    .uid(cls.getUid())
                    .contentDetail(contentDetailNode)
                    .views(cls.getViews())
                    .bookmarked(isBookmarked)
                    .build();
        }).collect(Collectors.toList());
    }

    public List<ClassResponseDTO> getClassesByPackageUidTest(String packageUid, Long userId) {
        List<ContentsEntity> classes = contentRepository.findClassesByPackageUid(packageUid);

        return classes.stream().map(cls -> {
            boolean isBookmarked = userId != null && cls.getBookmarkUsersList().contains(userId);
            System.out.println("isBookmarked = " + isBookmarked);

            JsonNode contentDetailNode = null;
            try {
                contentDetailNode = objectMapper.readTree(cls.getContentDetail());
            } catch (Exception e) {
                log.error("Failed to parse contentDetail", e);
            }

            return ClassResponseDTO.builder()
                    .id(cls.getId())
                    .uid(cls.getUid())
                    .contentDetail(contentDetailNode)
                    .views(cls.getViews())
                    .bookmarked(isBookmarked)
                    .build();
        }).collect(Collectors.toList());
    }

    public ClassResponseDTO getClassByUidTest(String uid, Long userId) {
        ContentsEntity clazz = contentRepository.findByContentTypeAndUid("class", uid)
                .orElseThrow(() -> new RuntimeException("클래스가 존재하지 않음"));

        try {
            JsonNode contentDetailNode = objectMapper.readTree(clazz.getContentDetail());

            boolean isBookmarked = false;
            if (userId != null) {
                isBookmarked = clazz.getBookmarkUsersList().contains(userId);
            }
            System.out.println("isBookmarked = " + isBookmarked);
            return ClassResponseDTO.builder()
                    .id(clazz.getId())
                    .uid(clazz.getUid())
                    .views(clazz.getViews())
                    .contentDetail(contentDetailNode) // ✅ JsonNode 그대로 넣기
                    .bookmarked(isBookmarked)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("contentDetail 파싱 실패", e);
        }
    }

    public List<ContentsEntity> getPopularClasses() {
        return contentRepository.findPopularClasses();
    }

}