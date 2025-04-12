package com.project.moduleserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class ModuleServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ModuleServerApplication.class, args);
    }

}
