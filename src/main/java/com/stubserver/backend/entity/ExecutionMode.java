package com.stubserver.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "VS_EXECUTIONMODE")
public class ExecutionMode {
    @Id
    @Column(name = "VSID")
    private Long vsid;

    @Column(name = "MASTERID")
    private Long masterId;

    @Column(name = "VIRTSERVER")
    private String virtServer;

    @Column(name = "EXECUTIONMODE")
    private String executionMode;
}
