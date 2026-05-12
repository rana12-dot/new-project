package com.stubserver.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "READYAPI_RESPONSE_TIME")
public class ResponseTime {
    @Id
    @Column(name = "RESPID")
    private Long respId;

    @Column(name = "METRICSID")
    private Long metricsId;

    @Column(name = "STARTTIME")
    private Timestamp startTime;

    @Column(name = "ENDTIME")
    private Timestamp endTime;

    @Column(name = "AVGRESPTIME")
    private Double avgRespTime;

    @Column(name = "MAXRESPTIME")
    private Double maxRespTime;

    @Column(name = "AVGTPS")
    private Double avgTps;
}
