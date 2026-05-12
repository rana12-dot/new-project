package com.stubserver.backend.controller;

import com.stubserver.backend.entity.User;
import com.stubserver.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/backend/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/getUsersList")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<List<Map<String, Object>>> getUsersList() {
        List<User> users = userService.getUsersList();
        List<Map<String, Object>> result = users.stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("USERNAME", u.getUsername());
            m.put("EMAIL", u.getEmail());
            m.put("FIRSTNAME", u.getFirstname());
            m.put("LASTNAME", u.getLastname());
            m.put("USERROLE", u.getUserrole());
            return m;
        }).toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/signUp")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Map<String, String>> signUp(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(userService.signUp(
                body.get("username"), body.get("email"), body.get("firstname"),
                body.get("lastname"), body.get("requestedBy"), body.get("userrole")));
    }

    @PostMapping("/signUp-modify")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Map<String, String>> modifyUser(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(userService.modifyUser(
                body.get("username"), body.get("firstname"), body.get("lastname"),
                body.get("email"), body.get("updatedBy"), body.get("userrole")));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Map<String, String>> deleteUser(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(userService.deleteUser(body.get("username"), body.get("requestedBy")));
    }
}
