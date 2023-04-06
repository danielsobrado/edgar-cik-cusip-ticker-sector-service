package com.jds.edgar.cik.download.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jds.edgar.cik.download.config.EdgarConfig;
import com.jds.edgar.cik.download.model.Stock;
import com.jds.edgar.cik.download.model.StockId;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "edgar.use-tickers-exchange", havingValue = "true")
public class CikExchangeDownloadServiceImpl extends AbstractDownloadService {

    private final EdgarConfig edgarConfig;
    private final StockRepository stockCikRepository;

    private static final String PROCESS_NAME = "CIK_DATA_UPDATE";

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
            Long cik = Long.valueOf(String.valueOf(stockValues.get(0)));
            String ticker = (String) stockValues.get(2);
            StockId stockId = new StockId(cik, ticker);
            Optional<Stock> optionalStockCik = stockCikRepository.findById(stockId);

            Stock stockCik = optionalStockCik.orElseGet(() -> {
                Stock newStockCik = Stock.builder()
                        .cik(cik)
                        .ticker(ticker)
                        .name((String) stockValues.get(1))
                        .exchange((String) stockValues.get(3))
                        .build();
                stockCikRepository.save(newStockCik);
                log.info("New Stock object saved: {}", newStockCik);
                return newStockCik;
            });

            Stock originalStockCik = stockCik.copy();
            boolean updated = false;

            String newExchange = (String) stockValues.get(3);
            if (stockCik.getExchange() == null && newExchange != null || stockCik.getExchange() != null && !stockCik.getExchange().equals(newExchange)) {
                stockCik.setExchange(newExchange);
                updated = true;
            }

            if (!stockCik.getName().equals(stockValues.get(1))) {
                stockCik.setName((String) stockValues.get(1));
                updated = true;
            }

            if (updated) {
                stockCik.setUpdated(LocalDateTime.now());
                stockCikRepository.save(stockCik);
                log.warn("Stock ID {} has been updated", stockId);
                log.info("Stock object before update: {}", originalStockCik);
                log.info("Stock object after update: {}", stockCik);
            }
        });
        updateLastExecutionTime(PROCESS_NAME);
    }

}
