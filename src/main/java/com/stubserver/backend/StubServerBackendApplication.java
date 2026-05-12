package com.stubserver.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class StubServerBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(StubServerBackendApplication.class, args);
    }
}
