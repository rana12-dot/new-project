package com.stubserver.backend.controller;

import com.stubserver.backend.service.ExecutionModeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/backend/api")
@RequiredArgsConstructor
public class ExecutionModeController {

    private final ExecutionModeService service;

    @GetMapping("/getExecutionMode")
    @PreAuthorize("hasAnyRole('Admin','ApplicationUser')")
    public ResponseEntity<Map<String, Object>> getExecutionMode(@RequestParam String serviceName,
                                                                 @RequestParam String serverIP) {
        return ResponseEntity.ok(service.getExecutionMode(serviceName, serverIP));
    }

    @PostMapping("/updateExecutionMode")
    @PreAuthorize("hasAnyRole('Admin','ApplicationUser')")
    public ResponseEntity<Map<String, String>> updateExecutionMode(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.updateExecutionMode(
                body.get("serviceName"), body.get("serverIP"), body.get("executionMode")));
    }

    @PostMapping("/addLiveURLs")
    @PreAuthorize("hasAnyRole('Admin','ApplicationUser')")
    public ResponseEntity<Map<String, String>> addLiveUrl(@RequestBody Map<String, String> body) {
        Map<String, String> result = service.addLiveUrl(
                body.get("serviceName"), body.get("serverIP"), body.get("liveurl"), body.get("updatedBy"));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @DeleteMapping("/deleteLiveURL")
    @PreAuthorize("hasAnyRole('Admin','ApplicationUser')")
    public ResponseEntity<Map<String, String>> deleteLiveUrl(@RequestBody Map<String, Object> body) {
        Object vurlIdObj = body.get("vsurlid");
        Long vsurlid = vurlIdObj instanceof Number n ? n.longValue() : Long.parseLong(vurlIdObj.toString());
        return ResponseEntity.ok(service.deleteLiveUrl(vsurlid));
    }

    @PostMapping("/setActiveLiveURL")
    @PreAuthorize("hasAnyRole('Admin','ApplicationUser')")
    public ResponseEntity<Map<String, String>> setActiveLiveUrl(@RequestBody Map<String, Object> body) {
        Object vsurlIdObj = body.get("vsurlid");
        Object vsidObj = body.get("vsid");
        Long vsurlid = vsurlIdObj instanceof Number n ? n.longValue() : Long.parseLong(vsurlIdObj.toString());
        Long vsid = vsidObj instanceof Number n ? n.longValue() : Long.parseLong(vsidObj.toString());
        String active = (String) body.get("active");
        if (!"Y".equalsIgnoreCase(active != null ? active.trim() : "")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Only active='Y' supported"));
        }
        return ResponseEntity.ok(service.setActiveLiveUrl(vsurlid, vsid, (String) body.get("host"),
                (String) body.get("updatedby")));
    }
}
