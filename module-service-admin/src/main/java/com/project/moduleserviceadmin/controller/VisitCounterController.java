// 방문자 수 기록 및 수집용 백엔드 구현
package com.project.moduleserviceadmin.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/visit")
public class VisitCounterController {

    private final Counter visitCounter;

    public VisitCounterController(MeterRegistry meterRegistry) {
        this.visitCounter = Counter.builder("page_visits")
                .description("Number of page visits")
                .register(meterRegistry);
    }

    @PostMapping
    public ResponseEntity<String> countVisit() {
        visitCounter.increment();
        return ResponseEntity.ok("Visit recorded");
    }
}
