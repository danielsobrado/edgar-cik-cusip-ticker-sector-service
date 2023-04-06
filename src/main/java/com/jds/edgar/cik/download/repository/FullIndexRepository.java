package com.jds.edgar.cik.download.repository;

import com.jds.edgar.cik.download.model.FullIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FullIndexRepository extends JpaRepository<FullIndex, Long> {
    @Query("SELECT MAX(fi.dateFiled) FROM FullIndex fi")
    Optional<LocalDate> findLatestDateFiled();

    @Query("SELECT fi FROM FullIndex fi WHERE fi.formType = :formType")
    List<FullIndex> findAllByFormType(String formType);
}
