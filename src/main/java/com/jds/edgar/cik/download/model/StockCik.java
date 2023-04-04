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
@Table(name = "stock_cik")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockCik {

    @Id
    private Long cik;

    @Nonnull
    private String ticker;

    private String title;

    private String exchange;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime created;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updated;

    public StockCik copy() {
        return StockCik.builder()
                .cik(this.cik)
                .ticker(this.ticker)
                .exchange(this.exchange)
                .title(this.title)
                .updated(this.updated)
                .created(this.created)
                .build();
    }

}
