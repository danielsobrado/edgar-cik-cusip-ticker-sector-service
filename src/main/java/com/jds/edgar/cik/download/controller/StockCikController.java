package com.jds.edgar.cik.download.controller;

import com.jds.edgar.cik.download.model.StockCik;
import com.jds.edgar.cik.download.repository.CikRepository;
import com.jds.edgar.cik.download.service.EdgarSectorEnrichService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stocks")
public class StockCikController {

    @NonNull
    private CikRepository cikRepository;

    @NonNull
    private EdgarSectorEnrichService edgarSectorEnrichService;

    @GetMapping("/cik/{cik}")
    public ResponseEntity<StockCik> getByCik(@PathVariable Long cik) {
        Optional<StockCik> stockCikOptional = cikRepository.findById(cik);
        return stockCikOptional.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/ticker/{ticker}")
    public ResponseEntity<StockCik> getByTicker(@PathVariable String ticker) {
        Optional<StockCik> stockCikOptional = cikRepository.findByTicker(ticker);
        return stockCikOptional.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/sector/{sector}")
    public ResponseEntity<List<StockCik>> getBySector(@PathVariable String sector) {
        List<StockCik> stockCiks = cikRepository.findBySector(sector);
        if (stockCiks.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(stockCiks);
    }

    @GetMapping("/sic/{sic}")
    public ResponseEntity<List<StockCik>> getBySic(@PathVariable String sic) {
        List<StockCik> stockCiks = cikRepository.findBySic(sic);
        if (stockCiks.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(stockCiks);
    }

    @PostMapping("/enrich/ticker/{ticker}")
    public ResponseEntity<StockCik> enrichCikById(@PathVariable String ticker) {
        Optional<StockCik> stockCikOptional = edgarSectorEnrichService.enrichCik(ticker);
        return stockCikOptional.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
