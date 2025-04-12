package com.project.moduleserviceadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ModuleServiceAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(ModuleServiceAdminApplication.class, args);
    }

}
