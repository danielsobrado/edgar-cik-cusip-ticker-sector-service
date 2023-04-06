package com.jds.edgar.cik.download.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jds.edgar.cik.download.config.EdgarConfig;
import com.jds.edgar.cik.download.model.Stock;
import com.jds.edgar.cik.download.repository.StockRepository;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "edgar.use-tickers", havingValue = "true")
public class CikDownloadServiceImpl extends AbstractDownloadService {

    private static final String PROCESS_NAME = "CIK_DATA_UPDATE";

    private final EdgarConfig edgarConfig;
    private final StockRepository stockCikRepository;

    @Scheduled(cron = "${edgar.cik-update-cron}")
    @Override
    @Transactional
    public void downloadCikData() {
        log.info("Started to download CIK data from: {}", edgarConfig.getCompanyTickersUrl());

        Try.of(() -> new URL(edgarConfig.getCompanyTickersUrl()))
                .mapTry(url -> url.openConnection())
                .mapTry(con -> (HttpURLConnection) con)
                .andThenTry(con -> con.setRequestMethod("GET"))
                .mapTry(con -> con.getInputStream())
                .mapTry(inputStream -> new ObjectMapper().readValue(inputStream, Map.class))
                .onSuccess(data -> updateDatabase(data))
                .onFailure(throwable -> log.error("Error downloading company tickers JSON", throwable));
    }

    private void updateDatabase(Map<String, Map<String, Object>> data) {
        data.forEach((key, value) -> {
            Long cik = Long.valueOf(String.valueOf(value.get("cik_str")));
            Optional<Stock> stockCikOptional = stockCikRepository.findByCik(cik);

            if (stockCikOptional.isPresent()) {
                Stock stockCik = stockCikOptional.get();
                Stock originalStockCik = stockCik.copy();
                boolean updated = false;

                if (!stockCik.getTicker().equals(value.get("ticker"))) {
                    stockCik.setTicker((String) value.get("ticker"));
                    updated = true;
                }

                if (!stockCik.getName().equals(value.get("title"))) {
                    stockCik.setName((String) value.get("title"));
                    updated = true;
                }

                if (updated) {
                    stockCik.setUpdated(LocalDateTime.now());
                    stockCikRepository.save(stockCik);
                    log.warn("CIK {} has been updated", cik);
                    log.info("Stock object before update: {}", originalStockCik);
                    log.info("Stock object after update: {}", stockCik);
                }
            } else {
                Stock newStockCik = Stock.builder()
                        .cik(cik)
                        .ticker((String) value.get("ticker"))
                        .name((String) value.get("title"))
                        .build();
                stockCikRepository.save(newStockCik);
                log.info("New Stock object saved: {}", newStockCik);
            }
        });
        updateLastExecutionTime(PROCESS_NAME);
    }

}
