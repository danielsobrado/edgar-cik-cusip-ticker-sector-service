package com.jds.edgar.cik.download.service;

import com.jds.edgar.cik.download.config.EdgarConfig;
import com.jds.edgar.cik.download.model.StockCik;
import com.jds.edgar.cik.download.repository.CikRepository;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "edgar.use-sector-enrich", havingValue = "true")
public class EdgarSectorEnrichService {

    private final EdgarConfig edgarConfig;
    private final CikRepository cikRepository;

    @Scheduled(cron = "${edgar.sector-enrich-cron}")
    public void enrichNextCik() {
        cikRepository.findFirstBySectorIsNull()
                .ifPresent(stockCik -> enrichCik(stockCik.getTicker()));
    }

    public Optional<StockCik> enrichCik(String ticker) {

        return cikRepository.findByTicker(ticker)
                .flatMap(stockCik -> Try.of(() -> extractData(stockCik.getTicker()))
                        .toEither()
                        .map(stockCik::updateEnrichedData)
                        .fold(
                                throwable -> {
                                    log.error("Error enriching CIK: {]", throwable.getMessage());
                                    return Optional.<StockCik>empty();
                                },
                                enrichedCik -> Optional.of(cikRepository.save(enrichedCik))
                        ));
    }

    private StockCik.EnrichedData extractData(String ticker) throws IOException {
        String url = edgarConfig.getEnrichSectorUrl().replace("{cik}", String.valueOf(ticker));
        log.info("Enriching CIK: {} from: {}", ticker, url);

        Document doc = Jsoup.connect(url).get();

        // Check if "No matching Ticker Symbol." is present in the HTML content
        if (doc.text().contains("No matching Ticker Symbol.")) {
            return StockCik.EnrichedData.builder()
                    .sic("Not Found")
                    .sector("Not Found")
                    .build();
        }

        String sic = doc.select("p.identInfo a").first().ownText().strip();
        String sector = doc.select("p.identInfo").first().ownText();

        int sicIndex = sector.indexOf("SIC:");
        int stateLocationIndex = sector.indexOf("State location:");

        // Check if the SIC: substring is present in the text before performing the substring operation
        if (sicIndex != -1 && stateLocationIndex != -1) {
            sector = sector.substring(sicIndex + 4, stateLocationIndex).strip();
        } else {
            sector = "Not Available";
        }

        return StockCik.EnrichedData.builder()
                .sic(sic)
                .sector(sector)
                .build();
    }

}
