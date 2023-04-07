package com.jds.edgar.cik.download.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "edgar")
public class EdgarConfig {
    private String companyTickersUrl;
    private String companyTickersExchangeUrl;
    private String enrichSectorUrl;

    private String fullIndexUrl;

    private Boolean useTickers;

    private Boolean useTickersExchange;

    private Boolean useEnrichSector;

    private Long retryDelay = 5000L;
}