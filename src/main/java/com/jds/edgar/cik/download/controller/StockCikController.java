package com.jds.edgar.cik.download.controller;

import com.jds.edgar.cik.download.model.StockCik;
import com.jds.edgar.cik.download.repository.CikRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/stocks")
public class StockCikController {

    @Autowired
    private CikRepository cikRepository;

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
}
