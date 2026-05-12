package com.stubserver.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "AUTH_REFRESH_TOKENS")
public class RefreshToken {
    @Id
    @Column(name = "JTI")
    private String jti;

    @Column(name = "USERNAME")
    private String username;

    @Column(name = "EXPIRES_AT")
    private Timestamp expiresAt;

    @Column(name = "ISSUED_AT")
    private Timestamp issuedAt;

    @Column(name = "USER_AGENT")
    private String userAgent;

    @Column(name = "IP")
    private String ip;
}
