
package com.project.moduleserviceadmin.repository;

import com.project.moduleserviceadmin.entity.TrafficLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TrafficLogRepository extends JpaRepository<TrafficLog, Long> {

    // IP 주소별 트래픽 찾기
    List<TrafficLog> findByClientIpOrderByTimestampDesc(String clientIp);

    // 특정 기간 동안의, 페이징된 트래픽 로그 목록
    Page<TrafficLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    // IP별 요청 수 집계
    @Query("SELECT t.clientIp, COUNT(t) FROM TrafficLog t GROUP BY t.clientIp ORDER BY COUNT(t) DESC")
    List<Object[]> countRequestsByIp();

    // 상태 코드별 요청 수 집계
    @Query("SELECT t.statusCode, COUNT(t) FROM TrafficLog t GROUP BY t.statusCode ORDER BY t.statusCode")
    List<Object[]> countRequestsByStatusCode();

    // 특정 IP의 최근 활동
    @Query("SELECT t FROM TrafficLog t WHERE t.clientIp = ?1 ORDER BY t.timestamp DESC")
    List<TrafficLog> findRecentActivitiesByIp(String clientIp, Pageable pageable);
}
