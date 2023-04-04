package com.jds.edgar.cik.download.repository;

import com.jds.edgar.cik.download.model.ProcessExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessExecutionRepository extends JpaRepository<ProcessExecution, Long> {
    Optional<ProcessExecution> findByName(String name);
}
