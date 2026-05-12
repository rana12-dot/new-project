package com.stubserver.backend.repository;

import com.stubserver.backend.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    void deleteAllByUsername(String username);
    Optional<RefreshToken> findByJti(String jti);
}
