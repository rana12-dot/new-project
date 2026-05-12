package com.stubserver.backend.controller;

import com.stubserver.backend.security.JwtAuthFilter;
import com.stubserver.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/backend/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signIn")
    public ResponseEntity<Map<String, Object>> signIn(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request,
            HttpServletResponse response) {

        response.setHeader("Cache-Control", "no-store");

        String username = null, password = null;
        AuthService.BasicAuth basic = AuthService.extractBasicAuth(authHeader);
        if (basic != null) {
            username = basic.username();
            password = basic.password();
        } else if (body != null) {
            username = body.get("username");
            password = body.get("password");
        }

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or missing credentials"));
        }

        Map<String, Object> result = authService.signIn(username, password, request, response);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(HttpServletRequest request, HttpServletResponse response) {
        return ResponseEntity.ok(authService.refresh(request, response));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request,
                                                       HttpServletResponse response,
                                                       Authentication auth) {
        String username = auth != null ? auth.getName() : null;
        return ResponseEntity.ok(authService.logout(request, response, username));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.forgotPassword(body.get("username")));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.resetPassword(body.get("token"), body.get("newPassword")));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody Map<String, String> body,
                                                               Authentication auth) {
        String contextUsername = auth != null ? auth.getName() : null;
        return ResponseEntity.ok(authService.changePassword(
                contextUsername, body.get("token"),
                body.get("currentPassword"), body.get("newPassword")));
    }
}
