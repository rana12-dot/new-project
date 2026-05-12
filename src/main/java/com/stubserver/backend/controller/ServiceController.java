package com.stubserver.backend.controller;

import com.stubserver.backend.service.ServiceManagementService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/backend/api")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceManagementService service;

    @PostMapping("/assignServices")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Map<String, String>> assignServices(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> services = (List<String>) body.get("assignedServices");
        return ResponseEntity.ok(service.assignServices(
                (String) body.get("username"), services, (String) body.get("environment")));
    }

    @PostMapping("/getAssignedServices")
    @PreAuthorize("hasAnyRole('Admin','ApplicationUser')")
    public ResponseEntity<Map<String, Object>> getAssignedServices(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.getAssignedServices(body.get("USERNAME"), body.get("environment")));
    }

    @GetMapping("/getGroupTagsConfig")
    @PreAuthorize("hasAnyRole('Admin','ApplicationUser')")
    public ResponseEntity<Map<String, Object>> getGroupTagsConfig(@RequestParam String serviceName,
                                                                    @RequestParam(required = false) String env) {
        return ResponseEntity.ok(service.getGroupTagsConfig(serviceName, env));
    }

    @PostMapping("/updateGroup")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Map<String, Boolean>> updateGroup(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.updateGroup(body.get("serviceName"), body.get("group"), body.get("env")));
    }

    @PostMapping("/updateTags")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Map<String, Boolean>> updateTags(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> tags = (List<String>) body.get("tags");
        return ResponseEntity.ok(service.updateTags((String) body.get("serviceName"), tags, (String) body.get("env")));
    }

    @GetMapping("/getDatasourceLists")
    @PreAuthorize("hasAnyRole('Admin','ApplicationUser')")
    public ResponseEntity<Map<String, Object>> getDatasourceLists(@RequestParam String environment) {
        return ResponseEntity.ok(service.getDatasourceLists(environment));
    }

    @GetMapping("/getDatasets")
    @PreAuthorize("hasAnyRole('Admin','ApplicationUser')")
    public ResponseEntity<Map<String, Object>> getDatasets(@RequestParam String serviceName) {
        return ResponseEntity.ok(service.getDatasets(serviceName));
    }

    @GetMapping("/getDatasets/download")
    @PreAuthorize("hasAnyRole('Admin','ApplicationUser')")
    public void downloadDataset(@RequestParam String serviceName,
                                 @RequestParam String fileName,
                                 HttpServletResponse response) throws IOException {
        Path file = service.findDatasetFile(serviceName, fileName);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        Files.copy(file, response.getOutputStream());
    }

    @DeleteMapping("/getDatasets/delete")
    @PreAuthorize("hasAnyRole('Admin','ApplicationUser')")
    public ResponseEntity<Map<String, String>> deleteDataset(@RequestParam String serviceName,
                                                              @RequestParam String fileName) {
        return ResponseEntity.ok(service.deleteDataset(serviceName, fileName));
    }
}
