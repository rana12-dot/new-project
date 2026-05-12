package com.stubserver.backend.service;

import com.stubserver.backend.config.AppProperties;
import com.stubserver.backend.config.TableNames;
import com.stubserver.backend.exception.BadRequestException;
import com.stubserver.backend.repository.EnvTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServerService {

    private final AppProperties appProperties;
    private final TableNames tableNames;
    private final EnvTableRepository envRepo;

    private final AtomicBoolean javaAppRunning = new AtomicBoolean(false);
    private final AtomicReference<String> serverStartTime = new AtomicReference<>(null);

    public List<Map<String, Object>> getServerLists(String environment) {
        if (environment == null || (!environment.equalsIgnoreCase("QA") && !environment.equalsIgnoreCase("UAT"))) {
            throw new BadRequestException("Invalid or missing environment parameter");
        }
        String auditTable = tableNames.auditTable(environment);
        List<Map<String, Object>> rows = envRepo.getAuditLogsByEnvForServerList(auditTable);

        String status = "Stopped";
        Object lastUpdate = null;

        if (!rows.isEmpty()) {
            Map<String, Object> row = rows.get(0);
            lastUpdate = row.get("TIMESTAMP");
            status = "SERVER START".equals(row.get("ACTION_TYPE")) ? "Running" : "Stopped";
        }

        return List.of(Map.of(
                "SERVERNAME", appProperties.getCoreServer().getName(),
                "PORT", appProperties.getCoreServer().getPort(),
                "STATUS", status,
                "LASTUPDATE", lastUpdate != null ? lastUpdate : "",
                "ENVIRONMENT", environment.toUpperCase()
        ));
    }

    public Map<String, String> runBatch(String action, String servername) {
        if (action == null || servername == null) {
            throw new BadRequestException("Missing action or servername");
        }

        if ("Stop".equals(action)) {
            String batPath = appProperties.getBatch().getJavaStop();
            if (batPath == null || batPath.isEmpty()) {
                throw new RuntimeException("JAVA_STOP_BAT not configured");
            }
            try {
                new ProcessBuilder("cmd.exe", "/c", "start", "cmd", "/c", batPath)
                        .start();
            } catch (IOException e) {
                throw new RuntimeException("Failed to stop server: " + e.getMessage());
            }
            javaAppRunning.set(false);
            return Map.of("message", "Server Stopped");

        } else if ("Start".equals(action)) {
            if (javaAppRunning.get()) {
                return Map.of("message", "Java app running already");
            }
            String batPath = appProperties.getBatch().getJavaStart();
            if (batPath == null || batPath.isEmpty()) {
                throw new RuntimeException("JAVA_START_BAT not configured");
            }
            try {
                ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", batPath);
                pb.redirectErrorStream(true);
                Process p = pb.start();
                // Detach — read output in background to prevent blocking
                p.getInputStream().close();
            } catch (IOException e) {
                throw new RuntimeException("Failed to start server: " + e.getMessage());
            }
            javaAppRunning.set(true);
            return Map.of("message", "Server Started");

        } else {
            throw new BadRequestException("Invalid action");
        }
    }

    public Map<String, Object> getLiveStatus() {
        int port = appProperties.getCoreServer().getPort();
        boolean portInUse = isPortInUse(port);

        if (!portInUse) {
            serverStartTime.set(null);
            return Map.of("liveStatus", "Stopped");
        }

        String healthApi = appProperties.getHealthApi();
        if (healthApi == null || healthApi.isEmpty()) {
            return Map.of("liveStatus", "Stopped", "error", "HEALTH_API is not configured");
        }

        boolean healthy = checkHealth(healthApi);
        if (healthy) {
            if (serverStartTime.get() == null) {
                serverStartTime.set(java.time.Instant.now().toString());
            }
            return Map.of("liveStatus", "Running", "upAndRunning", serverStartTime.get());
        }

        serverStartTime.set(null);
        return Map.of("liveStatus", "Stopped");
    }

    public Map<String, String> serverTimeInfo() {
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(zone);
        String abbr = now.getZone().getDisplayName(java.time.format.TextStyle.SHORT,
                java.util.Locale.ENGLISH);
        // Clean up: keep only letters
        String letters = abbr.replaceAll("GMT|UTC", "").replaceAll("[^A-Za-z]", "");
        if (!letters.matches("[A-Za-z]{2,5}")) letters = "UTC";
        String localTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        return Map.of("serverTimeZone", letters.toUpperCase(), "serverLocalTime", localTime);
    }

    private boolean isPortInUse(int port) {
        try {
            ProcessBuilder pb = new ProcessBuilder("netstat", "-aon");
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            return output.toLowerCase().contains(":" + port) && output.toLowerCase().contains("listening");
        } catch (Exception e) {
            log.warn("netstat check failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean checkHealth(String url) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET().build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                String body = resp.body().trim().toLowerCase();
                return "success".equals(body);
            }
            return false;
        } catch (Exception e) {
            log.warn("Health check failed: {}", e.getMessage());
            return false;
        }
    }
}
