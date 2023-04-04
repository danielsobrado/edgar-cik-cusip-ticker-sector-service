package com.jds.edgar.cik.download.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jds.edgar.cik.download.config.EdgarConfig;
import com.jds.edgar.cik.download.model.StockCik;
import com.jds.edgar.cik.download.repository.CikRepository;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CikDownloadServiceImpl implements CikDownloadService {

    private final EdgarConfig edgarConfig;
    private final CikRepository cikRepository;

//    @Scheduled(cron = "${edgar.cik-update-cron}")
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
            String cik = String.valueOf(value.get("cik_str"));
            StockCik stockCik = cikRepository.findById(cik).orElse(null);

            if (stockCik == null) {
                StockCik newStockCik = StockCik.builder()
                        .cik(cik)
                        .ticker((String) value.get("ticker"))
                        .title((String) value.get("title"))
                        .build();
                cikRepository.save(newStockCik);
                log.info("New StockCik object saved: {}", newStockCik);
            } else {
                StockCik originalStockCik = stockCik.copy();
                boolean updated = false;

                if (!stockCik.getTicker().equals(value.get("ticker"))) {
                    stockCik.setTicker((String) value.get("ticker"));
                    updated = true;
                }

                if (!stockCik.getTitle().equals(value.get("title"))) {
                    stockCik.setTitle((String) value.get("title"));
                    updated = true;
                }

                if (updated) {
                    stockCik.setUpdated(LocalDateTime.now());
                    cikRepository.save(stockCik);
                    log.warn("CIK {} has been updated", cik);
                    log.info("StockCik object before update: {}", originalStockCik);
                    log.info("StockCik object after update: {}", stockCik);
                }
            }
        });
    }

}
