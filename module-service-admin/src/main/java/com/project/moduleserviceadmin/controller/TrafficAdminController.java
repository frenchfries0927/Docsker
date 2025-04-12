package com.project.moduleserviceadmin.controller;

import com.project.moduleserviceadmin.entity.TrafficLog;
import com.project.moduleserviceadmin.service.TrafficLogService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/traffic")
public class TrafficAdminController {
    //TrafficLogService: 트래픽 로그 관련 비즈니스 로직을 처리하는 서비스.
    private final TrafficLogService trafficLogService;

    public TrafficAdminController(TrafficLogService trafficLogService) {
        this.trafficLogService = trafficLogService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // IP별 요청 통계
        //서비스 계층에서 IP 주소별 요청 횟수 통계를 가져와 Map에 저장. 키는 IP 주소(String), 값은 요청 횟수(Long)임.
        Map<String, Long> ipStats = trafficLogService.getIpFrequencyStats();

        // 상태 코드별 통계
        //서비스 계층에서 HTTP 상태 코드별 발생 횟수 통계를 가져와 Map에 저장. 키는 상태 코드(Integer), 값은 발생 횟수(Long)임
        Map<Integer, Long> statusStats = trafficLogService.getStatusCodeStats();

        model.addAttribute("ipStats", ipStats);
        model.addAttribute("statusStats", statusStats);

        return "admin/traffic/dashboard";
    }

    @GetMapping("/logs")
    public String logs(
            //page와 size 파라미터를 URL 쿼리 스트링에서 받아옴. 기본값은 각각 0과 20.
            //startDate와 endDate 파라미터를 URL에서 받아옴. ISO 표준 날짜 시간 형식으로 파싱.
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endDate,
            Model model) {

        //지정된 기간의 트래픽 로그를 페이징하여 가져옴.
        Page<TrafficLog> logs = trafficLogService.getLogsForPeriod(startDate, endDate, page, size);
        //트래픽 로그 데이터, 현재 페이지 번호, 총 페이지 수를 Model에 추가합니다.
        model.addAttribute("logs", logs);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", logs.getTotalPages());

        return "admin/traffic/logs";
    }

    @GetMapping("/ip/{ipAddress}")
    public String ipDetails(@PathVariable String ipAddress, Model model) {
        //특정 IP 주소의 모든 트래픽 로그를 가져옵니다.
        List<TrafficLog> ipLogs = trafficLogService.getLogsByIp(ipAddress);

        model.addAttribute("ipAddress", ipAddress);
        model.addAttribute("logs", ipLogs);

        return "admin/traffic/ip-details";
    }

    // REST API로 트래픽 데이터 제공 (예: 관리자 페이지에서 차트 렌더링용)
    @GetMapping("/api/ipstats")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> getIpStats() {
        //IP 통계 데이터를 ResponseEntity로 감싸서 반환하는 메서드를 정의.
        return ResponseEntity.ok(trafficLogService.getIpFrequencyStats());
        //서비스에서 IP 통계를 가져와 HTTP 200(OK) 상태 코드와 함께 반환.
    }


    //서비스에서 상태 코드 통계를 가져와 HTTP 200(OK) 상태 코드와 함께 반환.
    @GetMapping("/api/statusstats")
    @ResponseBody
    public ResponseEntity<Map<Integer, Long>> getStatusStats() {
        return ResponseEntity.ok(trafficLogService.getStatusCodeStats());
    }
}