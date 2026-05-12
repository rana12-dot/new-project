package com.stubserver.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "READYAPI_MONTHLY_METRICS")
public class MonthlyMetrics {
    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "VSNAME")
    private String vsname;

    @Column(name = "MONTH")
    private String month;

    @Column(name = "YEAR")
    private String year;

    @Column(name = "COUNT")
    private Long count;

    @Column(name = "QACOUNT")
    private Long qaCount;

    @Column(name = "PERFCOUNT")
    private Long perfCount;
}
