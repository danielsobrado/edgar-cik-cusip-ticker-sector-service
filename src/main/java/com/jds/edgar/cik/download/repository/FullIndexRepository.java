package com.jds.edgar.cik.download.repository;

import com.jds.edgar.cik.download.model.FullIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface FullIndexRepository extends JpaRepository<FullIndex, Long> {
    @Query("SELECT MAX(fi.dateFiled) FROM FullIndex fi")
    Optional<String> findLatestDateFiled();

    @Query("SELECT DISTINCT f.formType FROM FullIndex f")
    Set<String> findDistinctFormTypes();

    List<FullIndex> findByFormType(String filingType);

}
