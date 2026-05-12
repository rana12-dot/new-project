package com.stubserver.backend.controller;

import com.stubserver.backend.service.PortService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/backend/api")
@RequiredArgsConstructor
public class PortController {

    private final PortService portService;

    @GetMapping("/getAppPortLists")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Map<String, Object>> getAppPortLists() {
        return ResponseEntity.ok(portService.getAppPortLists());
    }

    @PostMapping("/addAppPortDetails")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Map<String, String>> addAppPortDetails(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(portService.addAppPortDetails(
                body.get("appname"), body.get("port"), body.get("updatedby")));
    }

    @PostMapping("/modifyAppPortDetails")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Map<String, String>> modifyAppPortDetails(@RequestBody Map<String, Object> body) {
        Object portidObj = body.get("portid");
        Long portid = portidObj instanceof Number n ? n.longValue() : Long.parseLong(portidObj.toString());
        return ResponseEntity.ok(portService.modifyAppPortDetails(
                portid, (String) body.get("appname"), (String) body.get("port"), (String) body.get("updatedby")));
    }

    @DeleteMapping("/deleteAppPort")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Map<String, String>> deleteAppPort(@RequestBody Map<String, Object> body) {
        Object portidObj = body.get("portid");
        Long portid = portidObj instanceof Number n ? n.longValue() : Long.parseLong(portidObj.toString());
        return ResponseEntity.ok(portService.deleteAppPort(portid));
    }

    @PostMapping("/getAvailablePorts")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Map<String, Object>> getAvailablePorts(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(portService.getAvailablePorts(body.get("appName"), body.get("portRange")));
    }
}
