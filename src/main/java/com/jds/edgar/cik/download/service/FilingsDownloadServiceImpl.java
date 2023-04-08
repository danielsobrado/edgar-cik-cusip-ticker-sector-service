package com.jds.edgar.cik.download.service;

import com.jds.edgar.cik.download.config.EdgarConfig;
import com.jds.edgar.cik.download.model.CikCusipMaps;
import com.jds.edgar.cik.download.model.FullIndex;
import com.jds.edgar.cik.download.repository.CikCusipMapsRepository;
import com.jds.edgar.cik.download.repository.FullIndexRepository;
import com.jds.edgar.cik.download.repository.StockRepository;
import io.vavr.control.Try;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilingsDownloadServiceImpl {
    private final EdgarConfig edgarConfig;
    private final RestTemplate restTemplate;
    private final StockRepository stockCikRepository;
    private final FullIndexRepository fullIndexRepository;
    private final CikCusipMapsRepository cikCusipMapsRepository;

    public void processFillings(List<String> filingTypes) {
        downloadFullIndex();

        for (String filingType : filingTypes) {
            String targetFolder = filingType;
            List<FullIndex> targetFilings = fullIndexRepository.findByFormType(filingType);
            downloadFilingsOfType(filingType);
        }

        List<String> csvFiles = filingTypes.stream().map(filingType -> filingType + ".csv").collect(Collectors.toList());
        generateMappings(csvFiles);
    }

    public void downloadFullIndex() {
        log.info("Start downloading full index files");
        int currentYear = Year.now().getValue();

        Optional<String> latestDateString = fullIndexRepository.findLatestDateFiled();
        LocalDate latestDate = latestDateString.map(dateStr -> LocalDate.parse(dateStr)).orElse(LocalDate.of(1994, 1, 1));

        int startYear = latestDate.getYear();
        int startQuarter = (latestDate.getMonthValue() - 1) / 3 + 1;

        IntStream.range(startYear, currentYear + 1)
                .boxed()
                .flatMap(year -> IntStream.range(year == startYear ? startQuarter : 1, 5).mapToObj(q -> Pair.of(year, q)))
                .map(pair -> String.format("%s/%d/QTR%d/master.idx", edgarConfig.getFullIndexUrl(), pair.getLeft(), pair.getRight()))
                .forEach(url -> downloadAndProcessMasterIdx(url));

        log.info("Finished downloading full index files");
    }

    public void downloadIndexForYearAndQuarter(int year, int quarter) {
        log.info("Start downloading index file for year {} and quarter {}", year, quarter);
        if (quarter < 1 || quarter > 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quarter must be between 1 and 4");
        }

        String url = String.format("%s/%d/QTR%d/master.idx", edgarConfig.getFullIndexUrl(), year, quarter);
        downloadAndProcessMasterIdx(url);
        log.info("Finished downloading index file for year {} and quarter {}", year, quarter);
    }

    private void downloadAndProcessMasterIdx(String url) {
        int retries = 3;
        boolean success = false;
        while (!success && retries > 0) {
            log.info("Downloading master.idx file from URL: {}", url);
            try {
                ResponseEntity<byte[]> response = restTemplate.execute(url, HttpMethod.GET, null, responseExtractor -> {
                    if (responseExtractor.getStatusCode() == HttpStatus.OK) {
                        String contentEncoding = responseExtractor.getHeaders().getFirst("Content-Encoding");
                        InputStream inputStream = responseExtractor.getBody();
                        if ("gzip".equalsIgnoreCase(contentEncoding)) {
                            inputStream = new GZIPInputStream(inputStream);
                        }
                        String masterIdxContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                        return new ResponseEntity<>(masterIdxContent.getBytes(StandardCharsets.UTF_8), HttpStatus.OK);
                    } else {
                        return new ResponseEntity<>(null, responseExtractor.getStatusCode());
                    }
                });

                if (response.getStatusCode() == HttpStatus.OK) {
                    String masterIdxContent = new String(response.getBody(), StandardCharsets.UTF_8);
                    parseMasterIdxContent(masterIdxContent);
                    success = true;
                } else {
                    log.error("Failed to download master.idx from URL: {} Retrying... Remaining retries: {}", url, retries - 1);
                    retries--;
                    if (retries > 0) {
                        try {
                            Thread.sleep(edgarConfig.getRetryDelay()); // 5 seconds delay
                        } catch (InterruptedException ie) {
                            log.error("Thread sleep interrupted: {}", ie.getMessage(), ie);
                        }
                    }
                }
            } catch (RestClientException e) {
                log.error("Failed to download with error: {}", e.getMessage());
                log.error("Failed to download master.idx from URL: {} Retrying... Remaining retries: {}", url, retries - 1);
                retries--;
                if (retries > 0) {
                    try {
                        Thread.sleep(edgarConfig.getRetryDelay()); // 5 seconds delay
                    } catch (InterruptedException ie) {
                        log.error("Thread sleep interrupted: {}", ie.getMessage(), ie);
                    }
                }
            }
        }
    }

    @Transactional
    public void parseMasterIdxContent(String masterIdxContent) {
        log.info("Start parsing master.idx content");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(masterIdxContent.getBytes(StandardCharsets.UTF_8))))) {

            // Skip header lines until the line that contains "-----"
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("-----")) {
                    break;
                }
            }

            List<String[]> rows = reader.lines()
                    .filter(rowLine -> rowLine.contains(".txt"))
                    .map(rowLine -> rowLine.strip().split("\\|"))
                    .collect(Collectors.toList());

            for (String[] row : rows) {
                FullIndex fullIndex = FullIndex.builder()
                        .cik(Long.parseLong(row[0].trim()))
                        .companyName(row[1].trim())
                        .formType(row[2].trim())
                        .dateFiled(row[3].trim())
                        .filename(row[4].trim())
                        .build();
                log.debug("Saving full index entry: {}", fullIndex);
                fullIndexRepository.save(fullIndex);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error parsing master.idx content", e);
        }
        log.info("Finished parsing master.idx content");
    }


    public String downloadFilingsOfType(String filingType) {
        log.info("Start downloading {} filings", filingType);

        List<FullIndex> targetFilings = fullIndexRepository.findByFormType(filingType);
        int newFilings = 0;
        int existingFilings = 0;

        for (int i = 0; i < targetFilings.size(); i++) {
            FullIndex row = targetFilings.get(i);
            log.info("{} out of {}", i, targetFilings.size());

            String cik = String.valueOf(row.getCik());
            String date = row.getDateFiled().toString();
            String year = date.split("-")[0].trim();
            String month = date.split("-")[1].trim();
            String url = row.getFilename().trim();
            String accession = url.split("\\.")[0].split("-")[url.split("\\.")[0].split("-").length - 1];

            Path folderPath = Paths.get(edgarConfig.getFilingsFolder(), filingType, year + "_" + month);
            folderPath.toFile().mkdirs();

            String filePath = String.format("%s/%s/%s_%s/%s_%s_%s.txt", edgarConfig.getFilingsFolder(), filingType, year, month, cik, date, accession);
            File file = new File(filePath);
            if (file.exists()) {
                existingFilings++;
                continue;
            }

            ResponseEntity<byte[]> response = restTemplate.getForEntity(edgarConfig.getBaseUrl() + url, byte[].class);
            if (response.getStatusCode() == HttpStatus.OK) {
                try {
                    String contentEncoding = response.getHeaders().getFirst("Content-Encoding");
                    byte[] responseBody = response.getBody();
                    String content;

                    if (contentEncoding != null && contentEncoding.equalsIgnoreCase("gzip")) {
                        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(responseBody);
                        GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);
                        content = IOUtils.toString(gzipInputStream, StandardCharsets.UTF_8);
                    } else {
                        content = new String(responseBody, StandardCharsets.UTF_8);
                    }

                    try (FileWriter fileWriter = new FileWriter(file)) {
                        fileWriter.write(content);
                        newFilings++;
                    } catch (Exception e) {
                        log.error("{}: {} failed to download", cik, date, e);
                    }
                } catch (IOException e) {
                    log.error("Error decompressing content: {}", e.getMessage(), e);
                }
            } else {
                log.error("{}: {} failed to download", cik, date);
            }
        }
        log.info("Finished downloading {} filings", filingType);
        log.info("New filings: {}, Existing filings: {}", newFilings, existingFilings);
        return "Downloaded " + newFilings + " new filings and found " + existingFilings + " existing filings for " + filingType + ".";
    }

    public String downloadFilingsOfType13() {
        log.info("Start downloading filings containing 13 in form type");

        Set<String> allFormTypes = fullIndexRepository.findDistinctFormTypes();
        Set<String> targetFormTypes = allFormTypes.stream()
                .filter(formType -> formType.contains("13"))
                .collect(Collectors.toSet());

        StringBuilder result = new StringBuilder();
        int totalNewFilings = 0;
        int totalExistingFilings = 0;

        for (String formType : targetFormTypes) {
            String downloadResult = downloadFilingsOfType(formType);
            result.append(downloadResult).append(System.lineSeparator());

            String[] parts = downloadResult.split("\\s+");
            int newFilings = Integer.parseInt(parts[2]);
            int existingFilings = Integer.parseInt(parts[8]);

            totalNewFilings += newFilings;
            totalExistingFilings += existingFilings;
        }

        log.info("Finished downloading filings containing 13 in form type");
        result.append("Total new filings: ").append(totalNewFilings)
                .append(", Total existing filings: ").append(totalExistingFilings)
                .append(" for forms containing 13.");

        return result.toString();
    }


    public void generateMappings(List<String> csvFiles) {
        log.info("Start generating mappings");

        ArrayList<String[]> dataFrames = new ArrayList<>();

        csvFiles.forEach(csvFile -> {
            Try<List<String[]>> recordsTry = readCsvFile(csvFile);
            recordsTry.onSuccess(records -> dataFrames.addAll(records));
        });

        List<String[]> filteredData = filterAndTransformData(dataFrames.stream());

        // Save the filtered data to the CikCusipMaps table
        saveFilteredDataToTable(filteredData);
        log.info("Finished generating mappings");
    }

    private void saveFilteredDataToTable(List<String[]> filteredData) {
        for (String[] row : filteredData) {
            Long cik = Long.parseLong(row[0].trim());
            String cusip6 = row[1].trim();
            String cusip8 = row[2].trim();

            CikCusipMaps cikCusipMaps = CikCusipMaps.builder()
                    .cik(cik)
                    .cusip6(cusip6)
                    .cusip8(cusip8)
                    .build();

            cikCusipMapsRepository.save(cikCusipMaps);
        }
    }

    private Try<List<String[]>> readCsvFile(String csvFile) {
        return Try.of(() -> {
            try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
                return reader.lines()
                        .skip(1) // Skip header
                        .map(line -> line.split(","))
                        .collect(Collectors.toList());
            }
        });
    }

    private List<String[]> filterAndTransformData(java.util.stream.Stream<String[]> dataStream) {
        return dataStream
                .filter(columns -> columns[2].length() == 6 || columns[2].length() == 8 || columns[2].length() == 9)
                .filter(columns -> !columns[2].startsWith("000000") && !columns[2].startsWith("0001pt"))
                .map(columns -> {
                    String cusip6 = columns[2].substring(0, 6);
                    String cusip8 = columns[2].substring(0, 8);
                    return new String[]{columns[1], cusip6, cusip8};
                })
                .distinct()
                .collect(Collectors.toList());
    }

}
