package com.project.moduleserverconfig;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.config.server.EnableConfigServer;

@SpringBootApplication
@EnableConfigServer
@EnableDiscoveryClient
public class ModuleServerConfigApplication {

	public static void main(String[] args) {
		SpringApplication.run(ModuleServerConfigApplication.class, args);
	}

}
