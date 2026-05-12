package com.stubserver.backend.controller;

import com.stubserver.backend.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/backend/api")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/getAuditLogs")
    @PreAuthorize("hasAnyRole('Admin','ApplicationUser')")
    public ResponseEntity<Map<String, Object>> getAuditLogs(@RequestParam String environment) {
        return ResponseEntity.ok(auditService.getAuditLogs(environment));
    }

    @PostMapping("/logAudit")
    @PreAuthorize("hasAnyRole('Admin','ApplicationUser','Guest')")
    public ResponseEntity<Map<String, String>> logAudit(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(auditService.logAudit(
                body.get("username"), body.get("serviceName"),
                body.get("action"), body.get("environment"), body.get("remark")));
    }
}
