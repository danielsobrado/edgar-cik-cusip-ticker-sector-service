package com.jds.edgar.cik.download.service;

import com.jds.edgar.cik.download.model.CikCusipMaps;
import com.jds.edgar.cik.download.model.Stock;
import com.jds.edgar.cik.download.repository.CikCusipMapsRepository;
import com.jds.edgar.cik.download.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockEnrichmentServiceImpl {
    private final StockRepository stockCikRepository;
    private final CikCusipMapsRepository cikCusipMapsRepository;

    public void enrichStockWithCusips() {
        List<Stock> stockCiks = stockCikRepository.findAll();
        List<CikCusipMaps> cikCusipMaps = cikCusipMapsRepository.findAll();

        for (Stock stockCik : stockCiks) {
            CikCusipMaps matchingCikCusipMap = cikCusipMaps.stream()
                    .filter(cikCusipMap -> cikCusipMap.getCik().equals(stockCik.getCik()))
                    .findFirst()
                    .orElse(null);

            if (matchingCikCusipMap != null) {
                stockCik.setCusip6(matchingCikCusipMap.getCusip6());
                stockCik.setCusip8(matchingCikCusipMap.getCusip8());
                stockCikRepository.save(stockCik);
            }
        }
    }
}
