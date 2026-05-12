package com.stubserver.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "READYAPI_PORT_RANGE")
public class PortRange {
    @Id
    @Column(name = "PORTID")
    private Long portId;

    @Column(name = "APPNAME")
    private String appName;

    @Column(name = "PORTS")
    private String ports;

    @Column(name = "UPDATEDBY")
    private String updatedBy;

    @Column(name = "UPDATETIME")
    private Timestamp updateTime;
}
