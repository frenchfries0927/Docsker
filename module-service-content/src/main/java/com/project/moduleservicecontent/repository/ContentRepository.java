package com.project.moduleservicecontent.repository;

import com.project.moduleservicecontent.entity.ContentsEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContentRepository extends JpaRepository<ContentsEntity, Long> {

    // 기본 쿼리들
    @Query(value = "SELECT * FROM contents WHERE content_type = :contentType ORDER BY content_detail->>'name' ASC", nativeQuery = true)
    List<ContentsEntity> findByContentType(@Param("contentType") String contentType);

    @Query(value = "SELECT * FROM contents WHERE content_detail->>'uid' = :uid", nativeQuery = true)
    Optional<ContentsEntity> findByUid(@Param("uid") String uid);


    @Query(value = "SELECT * FROM contents WHERE content_type = :contentType AND content_detail ->> 'uid' = :uid", nativeQuery = true)
    Optional<ContentsEntity> findByContentTypeAndUid(@Param("contentType") String contentType, @Param("uid") String uid);

    // 버전 관련 쿼리
    @Query(value = "SELECT * FROM contents WHERE content_type = 'version' ORDER BY (content_detail ->> 'version')::int DESC LIMIT 1", nativeQuery = true)
    ContentsEntity findLatestVersion();

    // 모듈 관련 쿼리
    @Query(value = "SELECT * FROM contents WHERE content_type = 'module' AND content_detail ->> 'versionId' = :versionUid ORDER BY content_detail ->> 'name'", nativeQuery = true)
    List<ContentsEntity> findModulesByVersionUid(@Param("versionUid") String versionUid);

    // 패키지 관련 쿼리
    @Query(value = "SELECT * FROM contents WHERE content_type = 'package' AND content_detail ->> 'moduleId' = :moduleUid ORDER BY content_detail ->> 'name'", nativeQuery = true)
    List<ContentsEntity> findPackagesByModuleUid(@Param("moduleUid") String moduleUid);

    // 클래스 관련 쿼리
    @Query(value = "SELECT * FROM contents WHERE content_type = 'class' AND content_detail ->> 'packageId' = :packageUid ORDER BY content_detail ->> 'name'", nativeQuery = true)
    List<ContentsEntity> findClassesByPackageUid(@Param("packageUid") String packageUid);

    // 검색 관련 쿼리
//    @Query(value = "SELECT * FROM contents WHERE content_type = 'class' AND LOWER(content_detail ->> 'name') LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY content_detail ->> 'name'", nativeQuery = true)
    @Query(value = "SELECT * FROM contents WHERE content_type = 'class' AND LOWER(content_detail ->> 'name') LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY CASE WHEN LOWER(content_detail ->> 'name') LIKE LOWER(CONCAT(:name, '%')) THEN 1 ELSE 0 END DESC, content_detail ->> 'name' ASC", nativeQuery = true)
    List<ContentsEntity> searchClassesByName(@Param("name") String name);

    @Query(value = "SELECT * FROM contents WHERE content_type = 'class' AND content_detail ->> 'name' = :name LIMIT 1", nativeQuery = true)
    Optional<ContentsEntity> findClassByExactName(@Param("name") String name);

    @Query(value = "SELECT * FROM contents WHERE content_type = 'package' AND LOWER(content_detail ->> 'name') LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY content_detail ->> 'name'", nativeQuery = true)
    List<ContentsEntity> searchPackagesByName(@Param("name") String name);

    // 타입별 클래스 쿼리
    @Query(value = "SELECT * FROM contents WHERE content_type = 'class' AND content_detail ->> 'type' = :type ORDER BY content_detail ->> 'name'", nativeQuery = true)
    List<ContentsEntity> findClassesByType(@Param("type") String type);

    // 계층 구조 내비게이션 쿼리
    @Query(value = """
        WITH RECURSIVE class_hierarchy AS (
            SELECT c.id, c.content_type, c.content_detail
            FROM contents c
            WHERE c.content_type = 'class' AND c.content_detail ->> 'uid' = :classUid
            
            UNION
            
            SELECT c.id, c.content_type, c.content_detail
            FROM contents c
            JOIN class_hierarchy ch ON 
                (c.content_type = 'package' AND c.content_detail ->> 'uid' = ch.content_detail ->> 'packageId')
                OR (c.content_type = 'module' AND c.content_detail ->> 'uid' = ch.content_detail ->> 'moduleId')
                OR (c.content_type = 'version' AND c.content_detail ->> 'uid' = ch.content_detail ->> 'versionId')
        )
        
        SELECT * FROM class_hierarchy
        """, nativeQuery = true)
    List<ContentsEntity> findClassHierarchy(@Param("classUid") String classUid);

//    --------------------
//    TechBLog

    // TechBlog 전체 조회
    @Query(value = "SELECT * FROM contents WHERE content_type = 'TechBlog' ORDER BY (content_detail ->> 'provider')", nativeQuery = true)
    List<ContentsEntity> findAllTechBlog();

    // TechBlog provider별 조회
    @Query(value = "SELECT * FROM contents WHERE content_type = 'TechBlog' AND content_detail ->> 'provider' ILIKE :provider ORDER BY content_detail ->> 'publishDate' DESC", nativeQuery = true)
    List<ContentsEntity> findbyTechBlogprovider(@Param("provider") String provider);

    @Transactional
    @Modifying
    @Query("UPDATE ContentsEntity c SET c.views = c.views + 1 WHERE c.id = :id")
    void incrementViews(Long id);

    @Query(value = """
    SELECT *
    FROM contents
    WHERE content_type = 'TechBlog'
    AND (:provider IS NULL OR content_detail->>'provider' = :provider)
    ORDER BY
        CASE WHEN :sort = 'views' THEN views END DESC,
        CASE WHEN :sort = 'latest' THEN (content_detail->>'publishDate')::timestamp END DESC,
        CASE WHEN :sort = 'bookmarks' THEN jsonb_array_length(bookmark_users) END DESC,
        (content_detail->>'publishDate')::timestamp DESC
    """,
            countQuery = """
    SELECT COUNT(*)
    FROM contents
    WHERE content_type = 'TechBlog'
    AND (:provider IS NULL OR content_detail->>'provider' = :provider)
    """,
            nativeQuery = true)
    Page<ContentsEntity> findTechBlogs(
            @Param("provider") String provider,
            @Param("sort") String sort,
            Pageable pageable
    );

    @Query(value = "SELECT * FROM contents WHERE content_type = 'class' ORDER BY views DESC LIMIT 3", nativeQuery = true)
    List<ContentsEntity> findPopularClasses();
}