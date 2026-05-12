package com.stubserver.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String corsOrigin;
    private String prodUrl;
    private CoreServer coreServer = new CoreServer();
    private Jwt jwt = new Jwt();
    private String serverLogDir;
    private String reqrespLogDir;
    private String datasetPaths;
    private Batch batch = new Batch();
    private String healthApi;
    private String dbType;

    @Getter @Setter
    public static class CoreServer {
        private int port = 9093;
        private String name = "StubServer";
    }

    @Getter @Setter
    public static class Jwt {
        private String accessSecret;
        private String refreshSecret;
        private String resetSecret;
        private long accessExpiresSeconds = 900;
        private long refreshExpiresSeconds = 1296000;
        private long resetExpiresSeconds = 900;
    }

    @Getter @Setter
    public static class Batch {
        private String javaStart;
        private String javaStop;
    }
}
