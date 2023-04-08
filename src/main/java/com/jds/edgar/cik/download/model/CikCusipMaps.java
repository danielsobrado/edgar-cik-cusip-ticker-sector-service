package com.jds.edgar.cik.download.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cik_cusip_maps")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CikCusipMaps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private Long cik;
    private String cusip6;
    private String cusip8;
}
