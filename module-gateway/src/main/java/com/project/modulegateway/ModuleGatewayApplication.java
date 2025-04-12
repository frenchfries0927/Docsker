package com.project.modulegateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(AuthorizationFilterConfig.class)
public class ModuleGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ModuleGatewayApplication.class, args);
    }

}
