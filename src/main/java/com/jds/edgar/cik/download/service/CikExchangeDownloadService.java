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

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CikExchangeDownloadService {

    private final EdgarConfig edgarConfig;
    private final CikRepository cikRepository;

    @Scheduled(cron = "${edgar.cik-exchange-update-cron}")
    public void updateCikExchangeData() {
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

            if (stockCik != null) {
                stockCik.setExchange((String) stockValues.get(3));
                cikRepository.save(stockCik);
            }
        });
    }
}
