package com.stubserver.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Date;

@Getter
@Setter
@Entity
@Table(name = "READYAPI_DAILY_METRICS")
public class DailyMetrics {
    @Id
    @Column(name = "METRICSID")
    private Long metricsId;

    @Column(name = "VSNAME")
    private String vsname;

    @Column(name = "VIRTSERVERNAME")
    private String virtServerName;

    @Column(name = "ENVTYPE")
    private String envType;

    @Column(name = "COUNT")
    private Long count;

    @Column(name = "TRANSDATE")
    private Date transDate;
}
