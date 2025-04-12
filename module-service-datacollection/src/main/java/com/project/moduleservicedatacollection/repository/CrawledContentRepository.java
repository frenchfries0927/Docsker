package com.project.moduleservicedatacollection.repository;

import com.project.moduleservicedatacollection.entity.CrawledContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrawledContentRepository extends JpaRepository<CrawledContent, Long> {
    List<CrawledContent> findByContentType(String contentType);
}
