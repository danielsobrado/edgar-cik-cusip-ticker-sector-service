package com.jds.edgar.cik.download.repository;

import com.jds.edgar.cik.download.model.StockCik;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CikRepository extends JpaRepository<StockCik, Long> {
    Optional<StockCik> findByTicker(String ticker);

    List<StockCik> findBySector(String sector);

    List<StockCik> findBySic(String sic);

    Optional<StockCik> findFirstBySectorIsNull();

    @Query("SELECT COUNT(s) FROM StockCik s WHERE s.sector IS NULL")
    long countBySectorIsNull();
}
