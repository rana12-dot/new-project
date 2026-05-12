package com.stubserver.backend.service;

import com.stubserver.backend.config.AppProperties;
import com.stubserver.backend.config.TableNames;
import com.stubserver.backend.exception.BadRequestException;
import com.stubserver.backend.exception.NotFoundException;
import com.stubserver.backend.repository.EnvTableRepository;
import com.stubserver.backend.util.FilePathGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceManagementService {

    private final EnvTableRepository envRepo;
    private final TableNames tableNames;
    private final AppProperties appProperties;

    @Transactional
    public Map<String, String> assignServices(String username, List<String> services, String environment) {
        String table = tableNames.assignedServicesTable(environment);
        envRepo.deleteAssignedServices(table, username);
        if (!services.isEmpty()) {
            envRepo.insertAssignedServices(table, username, services);
        }
        return Map.of("message", "Services assigned successfully in " + table);
    }

    public Map<String, Object> getAssignedServices(String username, String environment) {
        String table = tableNames.assignedServicesTable(environment);
        List<String> services = envRepo.getAssignedServices(table, username);
        return Map.of("username", username, "assignedService", services);
    }

    public Map<String, Object> getGroupTagsConfig(String serviceName, String env) {
        String environment = (env != null && !env.isEmpty()) ? env.toUpperCase() : "QA";
        String table = tableNames.vsDetailsTable(environment);
        Map<String, Object> row = envRepo.getGroupTagsConfig(table, serviceName);
        String group = row.containsKey("GROUP") && row.get("GROUP") != null ? row.get("GROUP").toString() : "";
        String tagsRaw = row.containsKey("TAGS") && row.get("TAGS") != null ? row.get("TAGS").toString() : "";
        List<String> tags = tagsRaw.isEmpty() ? List.of() : Arrays.asList(tagsRaw.split(","));
        return Map.of("group", group, "tags", tags, "env", environment);
    }

    @Transactional
    public Map<String, Boolean> updateGroup(String serviceName, String group, String env) {
        if (!List.of("QA", "UAT").contains(env.toUpperCase())) {
            throw new BadRequestException("Invalid environment. Must be QA or UAT.");
        }
        String table = tableNames.vsDetailsTable(env);
        String groupVal = (group != null && !group.trim().isEmpty()) ? group : null;
        envRepo.updateGroup(table, serviceName, groupVal);
        return Map.of("success", true);
    }

    @Transactional
    public Map<String, Boolean> updateTags(String serviceName, List<String> tags, String env) {
        if (tags == null || !(tags instanceof List)) {
            throw new BadRequestException("Tags must be an array.");
        }
        if (!List.of("QA", "UAT").contains(env.toUpperCase())) {
            throw new BadRequestException("Invalid environment. Must be QA or UAT.");
        }
        List<String> trimmed = tags.stream().map(String::trim).toList();
        if (new HashSet<>(trimmed).size() != trimmed.size()) {
            throw new BadRequestException("Duplicate tags are not allowed.");
        }
        String tagString = trimmed.isEmpty() ? null : String.join(",", trimmed);
        String table = tableNames.vsDetailsTable(env);
        envRepo.updateTags(table, serviceName, tagString);
        return Map.of("success", true);
    }

    public Map<String, Object> getDatasourceLists(String environment) {
        if (environment == null || (!environment.equalsIgnoreCase("QA") && !environment.equalsIgnoreCase("UAT"))) {
            throw new BadRequestException("Invalid or missing environment parameter (use QA or UAT)");
        }
        String table = tableNames.vsDetailsTable(environment);
        List<Map<String, Object>> rows = envRepo.getDatasourceEnabled(table);
        List<Map<String, Object>> data = rows.stream().map(r -> Map.<String, Object>of(
                "serviceName", r.get("VSNAME"),
                "datasourceEnabled", true
        )).toList();
        return Map.of("environment", environment.toLowerCase(), "data", data);
    }

    // ===== Dataset file operations =====

    private List<Path> datasetRoots() {
        String raw = appProperties.getDatasetPaths();
        if (raw == null || raw.isEmpty()) return List.of();
        return Arrays.stream(raw.split("[;,]|" + java.io.File.pathSeparator))
                .map(String::trim).filter(s -> !s.isEmpty()).map(Path::of).toList();
    }

    public Map<String, Object> getDatasets(String serviceName) {
        if (!FilePathGuard.isSafeFileName(serviceName)) {
            throw new BadRequestException("Invalid serviceName.");
        }
        List<Map<String, Object>> results = new ArrayList<>();
        for (Path root : datasetRoots()) {
            Path svcDir = root.resolve(serviceName);
            if (!Files.isDirectory(svcDir)) continue;
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(svcDir)) {
                for (Path file : ds) {
                    if (!Files.isRegularFile(file)) continue;
                    try {
                        BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
                        results.add(Map.of(
                                "fileName", file.getFileName().toString(),
                                "size", attrs.size(),
                                "lastModified", attrs.lastModifiedTime().toInstant()
                        ));
                    } catch (IOException e) {
                        log.warn("Stat error: {}", file);
                    }
                }
            } catch (IOException e) {
                log.warn("Read dir error: {}", svcDir);
            }
        }

        if (results.isEmpty()) {
            throw new NotFoundException("No matching datasets found.");
        }

        results.sort((a, b) -> {
            java.time.Instant ia = (java.time.Instant) a.get("lastModified");
            java.time.Instant ib = (java.time.Instant) b.get("lastModified");
            return ib.compareTo(ia);
        });

        return Map.of("status", "success", "serviceName", serviceName,
                "count", results.size(), "datasetFiles", results);
    }

    public Path findDatasetFile(String serviceName, String fileName) {
        if (!FilePathGuard.isSafeFileName(serviceName) || !FilePathGuard.isSafeFileName(fileName)) {
            throw new BadRequestException("Invalid serviceName or fileName.");
        }
        for (Path root : datasetRoots()) {
            Path candidate = root.resolve(serviceName).resolve(fileName);
            if (Files.isRegularFile(candidate)) return candidate;
        }
        throw new NotFoundException("File not found.");
    }

    public Map<String, String> deleteDataset(String serviceName, String fileName) {
        Path file = findDatasetFile(serviceName, fileName);
        try {
            Files.delete(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + e.getMessage());
        }
        return Map.of("status", "success", "message", "File deleted successfully.",
                "serviceName", serviceName, "fileName", fileName);
    }
}
