package com.project.moduleserviceadmin.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import com.project.moduleserviceadmin.service.TrafficLogService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class RequestInterceptor implements HandlerInterceptor {
    //HandlerInterceptor 인터페이스를 구현하는 RequestInterceptor 클래스를 선언. 이 인터셉터는 HTTP 요청 처리 과정을 가로채서 추가 작업을 수행.
    private static final Logger logger = LoggerFactory.getLogger(RequestInterceptor.class);
    //로깅을 위한 Logger 인스턴스를 생성. 이 인터셉터 클래스 이름으로 로거를 구성.


    //HandlerInterceptor의 preHandle 메서드를 오버라이드합니다. 이 메서드는 컨트롤러 메서드가 호출되기 전에 실행됩니다.
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 요청 시작 시간 저장
        //현재 시간을 밀리초 단위로 가져와 request 객체의 속성으로 저장합 나중에 요청 처리 시간을 계산하는 데 사용됨.
        request.setAttribute("startTime", System.currentTimeMillis());

        // 클라이언트 IP 주소 가져오기
        String clientIp = getClientIp(request);
        request.setAttribute("clientIp", clientIp);
        //클라이언트의 IP 주소를 가져와서 request 객체의 속성으로 저장합니다.



        // 요청 정보 로깅
        //현재 시간, 클라이언트 IP, HTTP 메서드, 요청 URI를 포함한 요청 정보를 로그에 기록합니다.
        logger.info("[REQUEST] {} | {} | {} | {}",
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                clientIp,
                request.getMethod(),
                request.getRequestURI());
        //true를 반환하여 요청 처리 체인이 계속 진행되도록함. false를 반환하면 요청 처리가 중단됨.
        return true;
    }

    //HandlerInterceptor의 postHandle 메서드를 오버라이드. 이 메서드는 컨트롤러 메서드가 실행된 후, 뷰가 렌더링되기 전에 호출.
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // 응답 상태 코드 기록
        //HTTP 응답 상태 코드를 로그에 기록합니다.
        logger.info("[RESPONSE] Status: {}", response.getStatus());
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //HandlerInterceptor의 afterCompletion 메서드를 오버라이드합니다. 이 메서드는 요청 처리가 완전히 끝나고 뷰 렌더링이 완료된 후에 호출됩니다.

        // 요청 처리 시간 계산
        //이전에 저장한 시작 시간을 가져와 현재 시간과의 차이를 계산하여 요청 처리 시간을 구함.
        long startTime = (Long) request.getAttribute("startTime");
        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;
        //  이전에 저장한 클라이언트 IP 주소를 가져옵니다.
        String clientIp = (String) request.getAttribute("clientIp");

        // 요청 처리 완료 로깅
        //요청 처리 완료 정보를 로그에 기록합니다. 시간, IP, 메서드, URI, 처리 시간, 상태 코드를 포함.
        logger.info("[COMPLETED] {} | {} | {} | {} | {} ms | Status: {}",
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                clientIp,
                request.getMethod(),
                request.getRequestURI(),
                processingTime,
                response.getStatus());

        // 트래픽 정보 서비스에 저장 (아래에서 구현할 TrafficLogService를 사용)
        //트래픽 정보를 TrafficLogService를 통해 데이터베이스에 저장. 클라이언트 IP, HTTP 메서드, URI, 상태 코드, 처리 시간, 요청 콘텐츠 길이를 전달.
        trafficLogService.saveTrafficLog(clientIp, request.getMethod(), request.getRequestURI(),
                response.getStatus(), processingTime, request.getContentLength());
    }

    // 클라이언트 IP 주소를 가져오는 메서드

    //먼저 "X-Forwarded-For" 헤더에서 IP를 확인합니다. 이는 프록시나 로드 밸런서를 통과한 경우 클라이언트의 실제 IP를 포함합니다.
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 로컬호스트 주소를 127.0.0.1로 표준화
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }

        return ip;
    }

    private final TrafficLogService trafficLogService;

    public RequestInterceptor(TrafficLogService trafficLogService) {
        this.trafficLogService = trafficLogService;
    }
}