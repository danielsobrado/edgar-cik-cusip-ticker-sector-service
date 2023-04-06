package com.jds.edgar.cik.download.repository;

import com.jds.edgar.cik.download.model.Stock;
import com.jds.edgar.cik.download.model.StockId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, StockId> {
    Optional<Stock> findByTicker(String ticker);

    Optional<Stock> findByCik(Long cik);

    List<Stock> findBySector(String sector);

    List<Stock> findBySic(String sic);

    Optional<Stock> findFirstBySectorIsNull();

    @Query("SELECT COUNT(s) FROM Stock s WHERE s.sector IS NULL")
    long countBySectorIsNull();
}
