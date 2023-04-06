package com.jds.edgar.cik.download.model;


import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "stocks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(StockId.class) // Add this line
public class Stock {

    @Id
    private Long cik;

    @Id
    @Nonnull
    private String ticker;

    private String name;

    private String sic;

    private String cusip6;

    private String cusip8;

    private String sector;

    private String exchange;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime created;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updated;

    public Stock copy() {
        return Stock.builder()
                .cik(this.cik)
                .ticker(this.ticker)
                .exchange(this.exchange)
                .name(this.name)
                .sic(this.sic)
                .cusip6(this.cusip6)
                .cusip8(this.cusip8)
                .sector(this.sector)
                .updated(this.updated)
                .created(this.created)
                .build();
    }

    public Stock updateEnrichedData(EnrichedData enrichedData) {
        setSic(enrichedData.getSic());
        setSector(enrichedData.getSector());
        return this;
    }

    @Data
    @Builder
    public static class EnrichedData {
        private String sic;
        private String sector;
    }

}
