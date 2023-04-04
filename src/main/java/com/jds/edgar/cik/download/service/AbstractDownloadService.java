package com.jds.edgar.cik.download.service;

import com.jds.edgar.cik.download.model.ProcessExecution;
import com.jds.edgar.cik.download.repository.ProcessExecutionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public abstract class AbstractDownloadService implements CikDownloadService {
    @Autowired
    private ProcessExecutionRepository processExecutionRepository;

    @PostConstruct
    public void init() {
        Optional<ProcessExecution> lastExecution = processExecutionRepository.findById(1L);

        if (lastExecution.isEmpty() || ChronoUnit.MONTHS.between(lastExecution.get().getLastExecution(), LocalDateTime.now()) > 1) {
            downloadCikData();
        }
    }

    void updateLastExecutionTime(String processName) {
        ProcessExecution processExecution = processExecutionRepository.findByName(processName).orElseGet(() -> {
            ProcessExecution newProcessExecution = new ProcessExecution();
            newProcessExecution.setName(processName);
            return newProcessExecution;
        });

        processExecution.setLastExecution(LocalDateTime.now());
        processExecutionRepository.save(processExecution);
    }
}
