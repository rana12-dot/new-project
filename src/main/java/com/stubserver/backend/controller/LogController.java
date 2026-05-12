package com.stubserver.backend.controller;

import com.stubserver.backend.service.LogService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/backend/api")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @PostMapping("/listLogFiles")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<List<Map<String, Object>>> listLogFiles(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(logService.listLogFiles(
                body.get("logType"), body.get("startDateTime"), body.get("endDateTime")));
    }

    @GetMapping("/downloadSingleLog")
    @PreAuthorize("hasRole('Admin')")
    public void downloadSingleLog(@RequestParam String fileName,
                                   HttpServletResponse response) throws IOException {
        logService.downloadSingleLog(fileName, response);
    }

    @PostMapping("/downloadSelectedLogs")
    @PreAuthorize("hasRole('Admin')")
    public void downloadSelectedLogs(@RequestBody Map<String, List<String>> body,
                                      HttpServletResponse response) throws IOException {
        logService.downloadSelectedLogs(body.get("files"), response);
    }

    @GetMapping("/reqresp/getLogFiles")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Map<String, Object>> getReqRespLogFiles(@RequestParam String serviceName) {
        return ResponseEntity.ok(logService.getReqRespLogFiles(serviceName));
    }

    @GetMapping("/reqresp/downloadLogFile")
    @PreAuthorize("hasRole('Admin')")
    public void downloadReqRespLogFile(@RequestParam String fileName,
                                        HttpServletResponse response) throws IOException {
        logService.downloadReqRespLogFile(fileName, response);
    }

    @GetMapping("/reqresp/downloadAllLogs")
    @PreAuthorize("hasRole('Admin')")
    public void downloadAllReqRespLogs(HttpServletResponse response) throws IOException {
        logService.downloadAllReqRespLogs(response);
    }
}
