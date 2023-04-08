package com.jds.edgar.cik.download.controller;

import com.jds.edgar.cik.download.model.Stock;
import com.jds.edgar.cik.download.repository.FullIndexRepository;
import com.jds.edgar.cik.download.repository.StockRepository;
import com.jds.edgar.cik.download.service.EdgarSectorEnrichServiceImpl;
import com.jds.edgar.cik.download.service.FilingsDownloadServiceImpl;
import com.jds.edgar.cik.download.service.StockEnrichmentServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stocks")
public class StockController {

    @NonNull
    private StockRepository stockCikRepository;

    @NonNull
    private EdgarSectorEnrichServiceImpl edgarSectorEnrichService;

    @NonNull
    private FilingsDownloadServiceImpl fullIndexDownloadService;

    @NonNull
    private StockEnrichmentServiceImpl stockEnrichmentService;

    @NonNull
    private FullIndexRepository fullIndexRepository;

    @NonNull
    private FilingsDownloadServiceImpl filingsDownloadService;

    @GetMapping("/cik/{cik}")
    public ResponseEntity<Stock> getByCik(@PathVariable Long cik) {
        Optional<Stock> stockCikOptional = stockCikRepository.findByCik(cik);
        return stockCikOptional.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/ticker/{ticker}")
    public ResponseEntity<Stock> getByTicker(@PathVariable String ticker) {
        Optional<Stock> stockCikOptional = stockCikRepository.findByTicker(ticker);
        return stockCikOptional.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping
    public ResponseEntity<List<Stock>> getByFilter(
            @RequestParam(value = "sector", required = false) String sector,
            @RequestParam(value = "sic", required = false) String sic) {
        List<Stock> stockCiks;
        if (sector != null) {
            stockCiks = stockCikRepository.findBySector(sector);
        } else if (sic != null) {
            stockCiks = stockCikRepository.findBySic(sic);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        if (stockCiks.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(stockCiks);
    }

    @PostMapping("/enrich/ticker/{ticker}")
    public ResponseEntity<Stock> enrichCikById(@PathVariable String ticker) {
        Optional<Stock> stockCikOptional = edgarSectorEnrichService.enrichCik(ticker);
        return stockCikOptional.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/export/csv")
    public void exportToCSV(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=stocks.csv");

        try (PrintWriter writer = response.getWriter()) {
            edgarSectorEnrichService.exportToCSV(writer);
        }
    }

    @GetMapping("/enrich/cusip")
    public ResponseEntity<Void> enrichStocksWithCusip() {
        try {
            stockEnrichmentService.enrichStockWithCusips();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/enrich/cusip/from-filings")
    public ResponseEntity<Void> generateMappingFile(@RequestParam(value = "filingTypes") String filingTypes) {
        try {
            List<String> filingTypesList = Arrays.asList(filingTypes.split(","));
            fullIndexDownloadService.processFillings(filingTypesList);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/download/{filingType}")
    public ResponseEntity<String> downloadFilingsOfType(@PathVariable String filingType) {
        String result = filingsDownloadService.downloadFilingsOfType(filingType);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/download/forms13")
    public ResponseEntity<String> downloadFilingsOfType13() {
        String result = filingsDownloadService.downloadFilingsOfType13();
        return new ResponseEntity<>(result, HttpStatus.OK);
    }


    @GetMapping("/formTypes")
    public ResponseEntity<Set<String>> getDistinctFormTypes() {
        Set<String> formTypes = fullIndexRepository.findDistinctFormTypes();
        return new ResponseEntity<>(formTypes, HttpStatus.OK);
    }

    @PostMapping("/downloadFullIndex")
    public ResponseEntity<String> downloadFullIndex() {
        filingsDownloadService.downloadFullIndex();
        return ResponseEntity.ok("Full index download initiated.");
    }

    @PostMapping("/download/index/{year}/{quarter}")
    public ResponseEntity<String> downloadIndexForYearAndQuarter(@PathVariable int year, @PathVariable int quarter) {
        try {
            filingsDownloadService.downloadIndexForYearAndQuarter(year, quarter);
            return ResponseEntity.ok("Index download initiated for year " + year + " and quarter " + quarter + ".");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }
}

