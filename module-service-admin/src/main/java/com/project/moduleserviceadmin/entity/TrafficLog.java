package com.project.moduleserviceadmin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "traffic_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrafficLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String clientIp;
    private String method;
    private String uri;
    private int statusCode;
    private long processingTimeMs;
    private int contentLength;
    private LocalDateTime timestamp;
}
