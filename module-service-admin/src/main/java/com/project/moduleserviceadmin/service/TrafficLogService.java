
package com.project.moduleserviceadmin.service;

import com.project.moduleserviceadmin.entity.TrafficLog;
import com.project.moduleserviceadmin.repository.TrafficLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TrafficLogService {

    private final TrafficLogRepository trafficLogRepository;

    public TrafficLogService(TrafficLogRepository trafficLogRepository) {
        this.trafficLogRepository = trafficLogRepository;
    }



    //빌더 패턴을 사용하여 TrafficLog 객체를 생성
    //TrafficLog 클래스가 Lombok의 @Builder 어노테이션을 사용하고 있음을 의미
    ///전달받은 매개변수 값과 현재 시간을 설정.
    public void saveTrafficLog(String clientIp, String method, String uri,
                               int statusCode, long processingTimeMs, int contentLength) {
        TrafficLog trafficLog = TrafficLog.builder()
                .clientIp(clientIp)
                .method(method)
                .uri(uri)
                .statusCode(statusCode)
                .processingTimeMs(processingTimeMs)
                .contentLength(contentLength)
                .timestamp(LocalDateTime.now())
                .build();

        trafficLogRepository.save(trafficLog);
    }

    //특정 클라이언트 IP 주소의 모든 트래픽 로그를 시간 역순(최신순)으로 조회하는 메서드.
    public List<TrafficLog> getLogsByIp(String clientIp) {
        return trafficLogRepository.findByClientIpOrderByTimestampDesc(clientIp);
    }
//특정 기간 동안의 트래픽 로그를 페이징하여 조회하는 메서드. 시작 시간, 종료 시간, 페이지 번호, 페이지 크기를 매개변수로 받음.
    public Page<TrafficLog> getLogsForPeriod(LocalDateTime start, LocalDateTime end, int page, int size) {
        //페이지 번호와 크기로 Pageable 객체를 생성합니다. 이 객체는 페이징 정보를 담고 있음.
        Pageable pageable = PageRequest.of(page, size);
        return trafficLogRepository.findByTimestampBetween(start, end, pageable);
        //시작 시간과 종료 시간 사이의 트래픽 로그를 페이징하여 조회.
    }

//    IP 주소별 요청 빈도 통계를 조회하는 메서드.


    public Map<String, Long> getIpFrequencyStats() {
        List<Object[]> results = trafficLogRepository.countRequestsByIp();
        //리포지토리에서 IP별 요청 수를 계산한 결과를 가져옴. 각 결과는 IP 주소와 요청 수를 포함하는 Object 배열.
        Map<String, Long> stats = new HashMap<>();
        //IP 주소를 키로, 요청 수를 값으로 하는 HashMap을 생성.
        for (Object[] result : results) {
            String ip = (String) result[0];
            Long count = (Long) result[1];
            stats.put(ip, count);
        }
//        조회 결과를 순회하면서 IP 주소와 요청 수를 HashMap에 저장

//IP 주소별 요청 수 통계가 담긴 Map을 반환.
        return stats;
    }

    //HTTP 상태 코드별 요청 빈도 통계를 조회하는 메서드.
    public Map<Integer, Long> getStatusCodeStats() {
        List<Object[]> results = trafficLogRepository.countRequestsByStatusCode();
        //리포지토리에서 상태 코드별 요청 수를 계산한 결과를 가져옴.
        Map<Integer, Long> stats = new HashMap<>();
        //상태 코드를 키로, 발생 횟수를 값으로 하는 HashMap을 생성.

        for (Object[] result : results) {
            Integer statusCode = (Integer) result[0];
            Long count = (Long) result[1];
            stats.put(statusCode, count);
        }
        //조회 결과를 순회하면서 상태 코드와 발생 횟수를 HashMap에 저장.
        //상태 코드별 발생 횟수 통계가 담긴 Map을 반환합니다.

        return stats;
    }

    //특정 IP 주소의 최근 활동을 제한된 개수만큼 조회하는 메서드. IP 주소와 조회할 로그 수를 매개변수로 받음.
    public List<TrafficLog> getRecentActivitiesByIp(String clientIp, int limit) {

        //첫 페이지(0)에서 지정된 개수(limit)만큼 데이터를 가져오기 위한 Pageable 객체를 생성.
        Pageable pageable = PageRequest.of(0, limit);
        //리포지토리에서 특정 IP 주소의 최근 활동을 제한된 개수만큼 조회합니다.
        return trafficLogRepository.findRecentActivitiesByIp(clientIp, pageable);
        //
    }

    //이 서비스 클래스는 트래픽 로그를 저장하고 다양한 방식으로 조회하는 비즈니스 로직을 제공합니다. 클라이언트 IP별, 시간대별 로그 조회와 IP 주소 및 상태 코드별 통계 기능을 담당합니다
}