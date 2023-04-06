package com.jds.edgar.cik.download.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class StockId implements Serializable {
    private Long cik;
    private String ticker;
}
