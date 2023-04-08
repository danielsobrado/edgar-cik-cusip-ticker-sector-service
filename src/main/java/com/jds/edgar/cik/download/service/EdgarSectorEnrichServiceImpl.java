package com.jds.edgar.cik.download.service;

import com.jds.edgar.cik.download.config.EdgarConfig;
import com.jds.edgar.cik.download.model.Stock;
import com.jds.edgar.cik.download.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "edgar.use-sector-enrich", havingValue = "true")
public class EdgarSectorEnrichServiceImpl {

    private final EdgarConfig edgarConfig;
    private final StockRepository cikRepository;

    @Scheduled(cron = "${edgar.sector-enrich-cron}")
    public void enrichNextCik() {
        cikRepository.findFirstBySectorIsNull()
                .ifPresent(stockCik -> enrichCik(stockCik.getTicker()));
    }

    public Optional<Stock> enrichCik(String ticker) {
        return cikRepository.findByTicker(ticker)
                .flatMap(stockCik -> {
                    int retries = stockCik.getLastError() != null ? 1 : 3;
                    return attemptEnrichCik(stockCik, retries);
                });
    }

    private Optional<Stock> attemptEnrichCik(Stock stockCik, int retries) {
        for (int attempt = 1; attempt <= retries; attempt++) {
            log.info("Attempt {} of {} for CIK: {}", attempt, retries, stockCik.getCik());
            try {
                Stock.EnrichedData enrichedData = extractData(stockCik.getTicker());
                stockCik.updateEnrichedData(enrichedData);
                stockCik.setLastError(null);
                return Optional.of(cikRepository.save(stockCik));
            } catch (IOException e) {
                log.error("Error enriching CIK: {} (attempt {}/{})", stockCik.getCik(), attempt, retries, e);
                stockCik.setLastError(e.getMessage());
                if (attempt < retries) {
                    try {
                        TimeUnit.SECONDS.sleep(5);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        cikRepository.save(stockCik);
        return Optional.empty();
    }

    private Stock.EnrichedData extractData(String ticker) throws IOException {
        String url = edgarConfig.getEnrichSectorUrl().replace("{cik}", String.valueOf(ticker));
        log.info("Enriching CIK: {} from: {}", ticker, url);

        Document doc = Jsoup.connect(url).get();

        // Check if "No matching Ticker Symbol." is present in the HTML content
        if (doc.text().contains("No matching Ticker Symbol.")) {
            log.warn("No matching Ticker Symbol for CIK: {}", ticker);
            return Stock.EnrichedData.builder()
                    .sic("Not Found")
                    .sector("Not Found")
                    .build();
        }

        // Check if "No matching CIK." is present in the HTML content
        if (doc.text().contains("No matching CIK.")) {
            log.warn("No matching CIK for CIK: {}", ticker);
            return Stock.EnrichedData.builder()
                    .sic("No CIK")
                    .sector("No CIK")
                    .build();
        }

        String sic = doc.select("p.identInfo a").first().ownText().strip();

        String sectorText = doc.select("p.identInfo").first().text();
        Pattern pattern = Pattern.compile("SIC: \\d{4} - (.*?) State location:");
        Matcher matcher = pattern.matcher(sectorText);

        String sector = "Not Available";
        if (matcher.find()) {
            sector = matcher.group(1).strip();
        }

        // Truncate the sector string to fit the database column
        int maxSectorLength = 100;
        if (sector.length() > maxSectorLength) {
            sector = sector.substring(0, maxSectorLength);
        }

        return Stock.EnrichedData.builder()
                .sic(sic)
                .sector(sector)
                .build();
    }

    public void exportToCSV(PrintWriter writer) {
        List<Stock> stockCiks = cikRepository.findAll();

        writer.println("CIK,Ticker,Name,Sector,SIC");

        stockCiks.stream().map(stockCik -> String.format("%s,%s,%s,%s,%s",
                        stockCik.getCik(),
                        stockCik.getTicker(),
                        stockCik.getName(),
                        stockCik.getSector(),
                        stockCik.getSic()))
                .forEach(writer::println);
    }

}
