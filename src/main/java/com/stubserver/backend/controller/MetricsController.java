package com.stubserver.backend.controller;

import com.stubserver.backend.service.MetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/backend/api")
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsService metricsService;

    @GetMapping("/getLifeTimeHits")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<List<Map<String, Object>>> getLifetimeHits() {
        return ResponseEntity.ok(metricsService.getLifetimeHits());
    }

    @PostMapping("/getMonthlyHits")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Map<String, Object>> getMonthlyHits(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(metricsService.getMonthlyHits(body.get("fromMonth"), body.get("toMonth")));
    }

    @PostMapping("/getCustomReport")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<List<Map<String, Object>>> getCustomReport(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(metricsService.getCustomReport(body.get("fromDate"), body.get("toDate")));
    }

    @PostMapping("/getDormantServiceLists")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Map<String, Object>> getDormantServiceLists(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(metricsService.getDormantServiceLists(body.get("serverIP")));
    }

    @PostMapping("/getResponseTime")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Map<String, Object>> getResponseTime(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(metricsService.getResponseTime(
                body.get("serviceName"), body.get("serverIP"),
                body.get("fromDateTime"), body.get("toDateTime")));
    }
}
