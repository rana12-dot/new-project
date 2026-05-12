package com.stubserver.backend.controller;

import com.stubserver.backend.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/backend/api")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/getServiceGroupTagsList")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Map<String, Object>> getServiceGroupTagsList() {
        return ResponseEntity.ok(catalogService.getServiceGroupTagsList());
    }

    @PostMapping("/masterCatalog/check")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Map<String, Object>> checkMasterCatalog(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(catalogService.checkMasterCatalog(
                body.get("serviceName"), body.get("port"), body.get("environment")));
    }
}
