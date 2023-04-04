package com.jds.edgar.cik.download.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jds.edgar.cik.download.config.EdgarConfig;
import com.jds.edgar.cik.download.model.StockCik;
import com.jds.edgar.cik.download.repository.CikRepository;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CikExchangeDownloadServiceImpl implements CikDownloadService {

    private final EdgarConfig edgarConfig;
    private final CikRepository cikRepository;

    @Override
    @Scheduled(cron = "${edgar.cik-exchange-update-cron}")
    @Transactional
    public void downloadCikData() {
        log.info("Started to download CIK data from: {}", edgarConfig.getCompanyTickersExchangeUrl());

        Try.of(() -> new URL(edgarConfig.getCompanyTickersExchangeUrl()))
                .mapTry(URL::openConnection)
                .mapTry(con -> (HttpURLConnection) con)
                .andThenTry(con -> con.setRequestMethod("GET"))
                .mapTry(HttpURLConnection::getInputStream)
                .mapTry(inputStream -> new ObjectMapper().readValue(inputStream, LinkedHashMap.class))
                .onSuccess(data -> updateDatabase(data))
                .onFailure(throwable -> log.error("Error downloading company tickers with exchange JSON", throwable));
    }

    private void updateDatabase(LinkedHashMap<String, Object> data) {
        List<List<Object>> stockData = (List<List<Object>>) data.get("data");
        stockData.forEach(stockValues -> {
            String cik = String.valueOf(stockValues.get(0));
            StockCik stockCik = cikRepository.findById(cik).orElse(null);

            if (stockCik == null) {
                stockCik = StockCik.builder()
                        .cik(cik)
                        .ticker((String) stockValues.get(2))
                        .title((String) stockValues.get(1))
                        .exchange((String) stockValues.get(3))
                        .build();
                cikRepository.save(stockCik);
                log.info("New StockCik object saved: {}", stockCik);
            } else {
                StockCik originalStockCik = stockCik.copy(); // Assuming you have a copy method in StockCik to create a deep copy of the object
                boolean updated = false;

                if (!stockCik.getExchange().equals(stockValues.get(3))) {
                    stockCik.setExchange((String) stockValues.get(3));
                    updated = true;
                }

                if (!stockCik.getTicker().equals(stockValues.get(2))) {
                    stockCik.setTicker((String) stockValues.get(2));
                    updated = true;
                }

                if (!stockCik.getTitle().equals(stockValues.get(1))) {
                    stockCik.setTitle((String) stockValues.get(1));
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
