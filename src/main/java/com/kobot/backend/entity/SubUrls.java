package com.kobot.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "tbl_suburls")
@Data
@NoArgsConstructor
public class SubUrls {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "suburl_id")
    private int subUrlId;

    @Column(name = "suburl")
    private String subUrl;

    @ManyToOne
    @JoinColumn(name = "hosturl_id")
    private HostUrl hostUrl;
}
