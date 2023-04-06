package com.jds.edgar.cik.download.service;

import com.jds.edgar.cik.download.config.EdgarConfig;
import com.jds.edgar.cik.download.model.CikCusipMaps;
import com.jds.edgar.cik.download.model.FullIndex;
import com.jds.edgar.cik.download.repository.CikCusipMapsRepository;
import com.jds.edgar.cik.download.repository.FullIndexRepository;
import com.jds.edgar.cik.download.repository.StockRepository;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
            List<FullIndex> targetFilings = fullIndexRepository.findAllByFormType(filingType);
            downloadFilingsOfType(filingType, targetFolder, targetFilings);
        }

        List<String> csvFiles = filingTypes.stream().map(filingType -> filingType + ".csv").collect(Collectors.toList());
        generateMappings(csvFiles);
    }

    public void downloadFullIndex() {
        int currentYear = Year.now().getValue();
        LocalDate latestDate = fullIndexRepository.findLatestDateFiled().orElse(LocalDate.of(1994, 1, 1));
        int startYear = latestDate.getYear();
        int startQuarter = (latestDate.getMonthValue() - 1) / 3 + 1;

        IntStream.range(startYear, currentYear + 1)
                .boxed()
                .flatMap(year -> IntStream.range(year == startYear ? startQuarter : 1, 5).mapToObj(q -> Pair.of(year, q)))
                .map(pair -> String.format("%s/%d/QTR%d/master.idx", edgarConfig.getFullIndexUrl(), pair.getLeft(), pair.getRight()))
                .map(url -> Try.of(() -> restTemplate.getForObject(url, String.class)).getOrElseThrow(e -> new RuntimeException("Failed to download master.idx", e)))
                .forEach(this::parseMasterIdxContent);
    }

    public void parseMasterIdxContent(String masterIdxContent) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(masterIdxContent.getBytes(StandardCharsets.UTF_8))))) {

            List<String[]> rows = reader.lines()
                    .filter(line -> line.contains(".txt"))
                    .map(line -> line.strip().split("\\|"))
                    .collect(Collectors.toList());

            for (String[] row : rows) {
                FullIndex fullIndex = FullIndex.builder()
                        .cik(Long.parseLong(row[0].trim()))
                        .companyName(row[1].trim())
                        .formType(row[2].trim())
                        .dateFiled(row[3].trim())
                        .filename(row[4].trim())
                        .build();
                fullIndexRepository.save(fullIndex);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error parsing master.idx content", e);
        }
    }


    public void downloadFilingsOfType(String filingType, String targetFolder, List<FullIndex> targetFilings) {
        log.info("Start downloading {} filings", filingType);

        for (int i = 0; i < targetFilings.size(); i++) {
            FullIndex row = targetFilings.get(i);
            log.info("{} out of {}", i, targetFilings.size());

            String cik = String.valueOf(row.getCik());
            String date = row.getDateFiled().toString();
            String year = date.split("-")[0].trim();
            String month = date.split("-")[1].trim();
            String url = row.getFilename().trim();
            String accession = url.split("\\.")[0].split("-")[-1];

            Path folderPath = Paths.get(targetFolder, year + "_" + month);
            folderPath.toFile().mkdirs();

            String filePath = String.format("./%s/%s_%s/%s_%s_%s.txt", targetFolder, year, month, cik, date, accession);
            File file = new File(filePath);
            if (file.exists()) {
                continue;
            }

            Try<String> contentTry = Try.of(() -> restTemplate.getForObject("https://www.sec.gov/Archives/" + url, String.class));
            if (contentTry.isSuccess()) {
                try (FileWriter fileWriter = new FileWriter(file)) {
                    fileWriter.write(contentTry.get());
                } catch (Exception e) {
                    log.error("{}: {} failed to download", cik, date, e);
                }
            } else {
                log.error("{}: {} failed to download", cik, date);
            }
        }
    }


    public void generateMappings(List<String> csvFiles) {
        ArrayList<String[]> dataFrames = new ArrayList<>();

        csvFiles.forEach(csvFile -> {
            Try<List<String[]>> recordsTry = readCsvFile(csvFile);
            recordsTry.onSuccess(records -> dataFrames.addAll(records));
        });

        List<String[]> filteredData = filterAndTransformData(dataFrames.stream());

        // Save the filtered data to the CikCusipMaps table
        saveFilteredDataToTable(filteredData);
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

    private void writeMappingFile(String outputFilePath, List<String[]> data) {
        try (FileWriter fileWriter = new FileWriter(outputFilePath)) {
            fileWriter.write("cik,cusip6,cusip8\n");
            for (String[] row : data) {
                fileWriter.write(String.join(",", row) + "\n");
            }
        } catch (Exception e) {
            log.error("Failed to write mapping file", e);
            throw new RuntimeException("Failed to write mapping file", e);
        }
    }

    private List<String[]> readTargetFilingsFromCsv(String fullIndexCsvPath, String filingType) {
        try (BufferedReader reader = new BufferedReader(new FileReader(fullIndexCsvPath))) {
            return reader.lines()
                    .skip(1) // Skip header
                    .map(line -> line.split(","))
                    .filter(columns -> columns[2].contains(filingType))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to read target filings from CSV", e);
        }
    }

}
