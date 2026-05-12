package com.stubserver.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final PasswordEncoder passwordEncoder;

    // In-memory used reset tokens (survives within JVM lifetime)
    private final Set<String> usedResetTokens = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public String hashPassword(String raw) {
        return passwordEncoder.encode(raw);
    }

    public boolean verifyPassword(String raw, String hash) {
        return passwordEncoder.matches(raw, hash);
    }

    public boolean isResetTokenUsed(String token) {
        return usedResetTokens.contains(token);
    }

    public void markResetTokenUsed(String token) {
        usedResetTokens.add(token);
    }
}
