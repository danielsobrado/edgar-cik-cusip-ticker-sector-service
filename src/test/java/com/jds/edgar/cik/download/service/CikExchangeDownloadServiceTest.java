package com.jds.edgar.cik.download.service;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DataJpaTest
@TestPropertySource(locations = "classpath:test.properties")
public class CikExchangeDownloadServiceTest {
    @Mock
    private EdgarConfig edgarConfig;

    @Mock
    private StockRepository cikRepository;

    @InjectMocks
    private CikExchangeDownloadServiceImpl cikExchangeDownloadService;

    private LinkedHashMap<String, Object> testData;

    @BeforeEach
    public void setUp() throws IOException {
        testData = new LinkedHashMap<>();
        // Load the test data from the manually downloaded file in the resources folder
        ObjectMapper objectMapper = new ObjectMapper();
        InputStream testDataInputStream = getClass().getResourceAsStream("/company_tickers_exchange_test.json");
        ExchangeTestData exchangeTestData = objectMapper.readValue(testDataInputStream, ExchangeTestData.class);
        List<List<Object>> data = exchangeTestData.getData();
        for (List<Object> entry : data) {
            testData.put(String.valueOf(entry.get(0)), entry);
        }
    }

    @Test
    public void testUpdateCikExchangeData() {
        when(edgarConfig.getCompanyTickersExchangeUrl()).thenReturn("classpath:company_tickers_exchange_test.json");
        when(cikRepository.findById(any())).thenReturn(Optional.of(mock(Stock.class)));

        cikExchangeDownloadService.downloadCikData();

        verify(cikRepository, atLeastOnce()).save(any(Stock.class));
    }

}