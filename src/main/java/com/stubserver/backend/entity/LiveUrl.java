package com.stubserver.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "VS_LIVEURLS")
public class LiveUrl {
    @Id
    @Column(name = "VSURLID")
    private Long vsurlId;

    @Column(name = "VSID")
    private Long vsid;

    @Column(name = "HOST")
    private String host;

    @Column(name = "ISACTIVE")
    private String isActive;

    @Column(name = "UPDATETIME")
    private Timestamp updateTime;

    @Column(name = "UPDATEDBY")
    private String updatedBy;
}
