package com.stubserver.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "READYAPI_VS_CATALOG")
public class VsCatalog {
    @Id
    @Column(name = "MASTERID")
    private Long masterId;

    @Column(name = "VSNAME")
    private String vsname;

    @Column(name = "VIRTSERVER")
    private String virtServer;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "PORT")
    private String port;

    @Column(name = "`GROUP`")
    private String group;

    @Column(name = "TAGS")
    private String tags;
}
