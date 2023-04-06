package com.jds.edgar.cik.download.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jds.edgar.cik.download.config.EdgarConfig;
import com.jds.edgar.cik.download.model.Stock;
import com.jds.edgar.cik.download.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DataJpaTest
@TestPropertySource(locations = "classpath:test_application.properties")
public class CikDownloadServiceTest {
    @Mock
    private EdgarConfig edgarConfig;

    @Mock
    private StockRepository cikRepository;

    @InjectMocks
    private CikDownloadServiceImpl cikDownloadService;

    private Map<String, Map<String, Object>> testData;

    @BeforeEach
    public void setUp() throws IOException {
        testData = new HashMap<>();

        // Load the test data from the manually downloaded file in the resources folder
        InputStream testDataInputStream = getClass().getResourceAsStream("/company_tickers_test.json");
        ObjectMapper objectMapper = new ObjectMapper();
        testData = objectMapper.readValue(testDataInputStream, new TypeReference<Map<String, Map<String, Object>>>() {});
    }


    @Test
    public void testUpdateCikData() {
        when(edgarConfig.getCompanyTickersUrl()).thenReturn("classpath:company_tickers_test.json");
        when(cikRepository.findById(any())).thenReturn(Optional.empty());

        cikDownloadService.downloadCikData();

        verify(cikRepository, atLeastOnce()).save(any(Stock.class));
    }

    @Test
    public void testUpdateExistingCikData() {
        when(edgarConfig.getCompanyTickersUrl()).thenReturn("classpath:company_tickers_test.json");
        when(cikRepository.findById(any())).thenReturn(Optional.of(mock(Stock.class)));

        cikDownloadService.downloadCikData();

        verify(cikRepository, atLeastOnce()).save(any(Stock.class));
    }

}