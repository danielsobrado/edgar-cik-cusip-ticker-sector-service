package com.jds.edgar.cik.download.repository;

import com.jds.edgar.cik.download.model.CikCusipMaps;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CikCusipMapsRepository extends JpaRepository<CikCusipMaps, Long> {
}
