package com.securitylog.service;

import com.securitylog.entity.LogEntry;
import com.securitylog.repository.LogEntryRepository;
import com.securitylog.util.TsvParser;
import com.securitylog.util.ZscalerParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

@Service
public class LogService {

    private static final Logger log = LoggerFactory.getLogger(LogService.class);

    private final LogEntryRepository logEntryRepository;

    public LogService(LogEntryRepository logEntryRepository) {
        this.logEntryRepository = logEntryRepository;
    }

    @Transactional
    public int ingest(MultipartFile file) throws IOException {
        String logName = file.getOriginalFilename();
        if (logName == null || logName.isBlank()) {
            logName = "upload-" + System.currentTimeMillis();
        }
        logName = logName.replaceAll("[/\\\\]", "_");

        // Detect format from the first non-blank line
        boolean isKV = detectKeyValue(file);
        BiFunction<String, String, LogEntry> parser = isKV ? ZscalerParser::parseLine : TsvParser::parseLine;
        log.info("Using {} parser for {}", isKV ? "key=value" : "TSV", logName);

        List<LogEntry> batch = new ArrayList<>();
        int totalSaved = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                LogEntry entry = parser.apply(line, logName);
                if (entry != null) {
                    batch.add(entry);
                }
                if (batch.size() >= 500) {
                    logEntryRepository.saveAll(batch);
                    totalSaved += batch.size();
                    batch.clear();
                }
            }
        }
        if (!batch.isEmpty()) {
            logEntryRepository.saveAll(batch);
            totalSaved += batch.size();
        }
        log.info("Ingested {} entries from {} using {} parser", totalSaved, logName, isKV ? "key=value" : "TSV");
        return totalSaved;
    }

    /** Returns true if the file looks like key=value format, false if tab-delimited. */
    private boolean detectKeyValue(MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    return line.contains("=");
                }
            }
        }
        return false;
    }

    public List<String> listLogNames() {
        return logEntryRepository.findDistinctLogNames();
    }

    // Returns entries ordered by record_id ASC, which matches file insertion order (chronological).
    public List<LogEntry> getEntriesByLogName(String logName) {
        return logEntryRepository.findByLogNameOrderByRecordIdAsc(logName);
    }
}
