package com.jds.edgar.cik.download.repository;

import com.jds.edgar.cik.download.model.StockCik;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CikRepository extends JpaRepository<StockCik, Long> {
    Optional<StockCik> findByTicker(String ticker);
}
