package com.stubserver.backend.service;

import com.stubserver.backend.config.AppProperties;
import com.stubserver.backend.exception.BadRequestException;
import com.stubserver.backend.exception.NotFoundException;
import com.stubserver.backend.util.FilePathGuard;
import com.stubserver.backend.util.ZipUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogService {

    private static final int MAX_ZIP_FILES = 120;

    private final AppProperties appProperties;

    // ===== Server logs =====

    public List<Map<String, Object>> listLogFiles(String logType, String startDateTime, String endDateTime) {
        if (logType == null || startDateTime == null || endDateTime == null) {
            throw new BadRequestException("Missing required parameters.");
        }
        String logDir = appProperties.getServerLogDir();
        if (logDir == null || logDir.isEmpty()) throw new RuntimeException("SERVER_LOG_DIR not configured.");

        Instant start = Instant.parse(startDateTime);
        Instant end = Instant.parse(endDateTime);

        try (Stream<Path> stream = Files.list(Path.of(logDir))) {
            List<Path> allSorted = stream
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        if ("error".equalsIgnoreCase(logType)) return name.startsWith("stubserver-error");
                        return name.startsWith("stubserver") && !name.startsWith("stubserver-error");
                    })
                    .sorted(Comparator.comparing(p -> {
                        try { return Files.getLastModifiedTime(p).toInstant(); } catch (IOException e) { return Instant.EPOCH; }
                    }))
                    .collect(Collectors.toList());

            List<Path> selected = new ArrayList<>();
            for (Path p : allSorted) {
                Instant mtime;
                try { mtime = Files.getLastModifiedTime(p).toInstant(); } catch (IOException e) { continue; }
                if (!mtime.isBefore(start) && !mtime.isAfter(end)) {
                    selected.add(p);
                } else if (mtime.isAfter(end)) {
                    selected.add(p);
                    break;
                }
            }

            return selected.stream().map(p -> {
                try {
                    BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", p.getFileName().toString());
                    m.put("size", attrs.size());
                    m.put("modifiedDate", attrs.lastModifiedTime().toInstant());
                    return m;
                } catch (IOException e) { return null; }
            }).filter(Objects::nonNull).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Internal Server Error: " + e.getMessage());
        }
    }

    public void downloadSingleLog(String fileName, HttpServletResponse response) throws IOException {
        String logDir = appProperties.getServerLogDir();
        if (logDir == null) throw new RuntimeException("LOG_DIR not configured.");
        if (!FilePathGuard.isSafeFileName(fileName)) throw new BadRequestException("Invalid file path.");

        Path filePath = Path.of(logDir, fileName).toAbsolutePath().normalize();
        if (!FilePathGuard.isPathInside(logDir, filePath.toString())) {
            throw new BadRequestException("Invalid file path.");
        }
        if (!Files.exists(filePath)) throw new NotFoundException("File not found.");

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        Files.copy(filePath, response.getOutputStream());
    }

    public void downloadSelectedLogs(List<String> files, HttpServletResponse response) throws IOException {
        String logDir = appProperties.getServerLogDir();
        if (logDir == null) throw new RuntimeException("LOG_DIR not configured.");
        if (files == null || files.isEmpty()) throw new BadRequestException("No files specified.");

        record FileEntry(Path path, Instant mtime) {}

        List<FileEntry> candidates = new ArrayList<>();
        for (String name : files) {
            if (!FilePathGuard.isSafeFileName(name)) continue;
            Path abs = Path.of(logDir, name).toAbsolutePath().normalize();
            if (!FilePathGuard.isPathInside(logDir, abs.toString())) continue;
            if (!Files.isRegularFile(abs)) continue;
            try {
                Instant mtime = Files.getLastModifiedTime(abs).toInstant();
                candidates.add(new FileEntry(abs, mtime));
            } catch (IOException e) { /* skip */ }
        }

        if (candidates.isEmpty()) throw new NotFoundException("No valid files to zip.");

        candidates.sort((a, b) -> b.mtime().compareTo(a.mtime()));
        boolean limitApplied = candidates.size() > MAX_ZIP_FILES;
        List<FileEntry> limited = candidates.subList(0, Math.min(candidates.size(), MAX_ZIP_FILES));

        String zipName = "logs_" + System.currentTimeMillis() + ".zip";
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + zipName + "\"");
        response.setHeader("Access-Control-Expose-Headers", "X-Limit-Applied, X-Original-Count, X-Selected-Count");
        response.setHeader("X-Original-Count", String.valueOf(candidates.size()));
        response.setHeader("X-Selected-Count", String.valueOf(limited.size()));
        response.setHeader("X-Limit-Applied", String.valueOf(limitApplied));

        ZipUtil.writeZip(limited.stream().map(FileEntry::path).collect(Collectors.toList()), response.getOutputStream());
    }

    // ===== Req/resp logs =====

    public Map<String, Object> getReqRespLogFiles(String serviceName) {
        String logDir = appProperties.getReqrespLogDir();
        if (logDir == null || logDir.isEmpty()) throw new RuntimeException("REQRESP_LOG_DIR not configured.");
        if (serviceName == null || serviceName.isEmpty()) throw new BadRequestException("Missing serviceName parameter.");

        try (Stream<Path> stream = Files.list(Path.of(logDir))) {
            List<Map<String, Object>> logFiles = stream
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.startsWith(serviceName) && name.endsWith(".log");
                    })
                    .map(p -> {
                        try {
                            Map<String, Object> m = new LinkedHashMap<>();
                            m.put("fileName", p.getFileName().toString());
                            m.put("lastModified", Files.getLastModifiedTime(p).toInstant());
                            return m;
                        } catch (IOException e) { return null; }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            return Map.of("status", "success", "serviceName", serviceName, "logFiles", logFiles);
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch log files: " + e.getMessage());
        }
    }

    public void downloadReqRespLogFile(String fileName, HttpServletResponse response) throws IOException {
        String logDir = appProperties.getReqrespLogDir();
        if (logDir == null) throw new RuntimeException("REQRESP_LOG_DIR not configured.");
        if (fileName == null) throw new BadRequestException("Missing fileName parameter.");

        Path filePath = Path.of(logDir, fileName);
        if (!Files.exists(filePath)) throw new NotFoundException("File not found.");

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        Files.copy(filePath, response.getOutputStream());
    }

    public void downloadAllReqRespLogs(HttpServletResponse response) throws IOException {
        String logDir = appProperties.getReqrespLogDir();
        if (logDir == null || logDir.isEmpty()) throw new RuntimeException("REQRESP_LOG_DIR not configured.");

        List<Path> logFiles;
        try (Stream<Path> stream = Files.list(Path.of(logDir))) {
            logFiles = stream.filter(p -> p.getFileName().toString().endsWith(".log"))
                    .collect(Collectors.toList());
        }

        if (logFiles.isEmpty()) throw new NotFoundException("No log files found.");

        String zipName = "all_logs_" + System.currentTimeMillis() + ".zip";
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + zipName + "\"");
        ZipUtil.writeZip(logFiles, response.getOutputStream());
    }
}
