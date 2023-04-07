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
    private FilingsDownloadServiceImpl filingsDownloadServiceImpl;

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

    @GetMapping("/sector/{sector}")
    public ResponseEntity<List<Stock>> getBySector(@PathVariable String sector) {
        List<Stock> stockCiks = stockCikRepository.findBySector(sector);
        if (stockCiks.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(stockCiks);
    }

    @GetMapping("/sic/{sic}")
    public ResponseEntity<List<Stock>> getBySic(@PathVariable String sic) {
        List<Stock> stockCiks = stockCikRepository.findBySic(sic);
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

    @GetMapping("/enrich/cusip/from-filings")
    public ResponseEntity<Void> generateMappingFile(@RequestParam(value = "filingTypes") String filingTypes) {
        try {
            List<String> filingTypesList = Arrays.asList(filingTypes.split(","));
            fullIndexDownloadService.processFillings(filingTypesList);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/download/{filingType}")
    public ResponseEntity<String> downloadFilingsOfType(@PathVariable String filingType) {
        String result = filingsDownloadServiceImpl.downloadFilingsOfType(filingType);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/formTypes")
    public ResponseEntity<Set<String>> getDistinctFormTypes() {
        Set<String> formTypes = fullIndexRepository.findDistinctFormTypes();
        return new ResponseEntity<>(formTypes, HttpStatus.OK);
    }
}

