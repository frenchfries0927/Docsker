package com.project.modulegateway;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@ConfigurationProperties(prefix = "authorization")
@Getter
@Setter
public class AuthorizationFilterConfig {
    private List<String> optionalPaths = new ArrayList<>();
}
