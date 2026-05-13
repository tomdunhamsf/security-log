package com.securitylog.service;

import com.securitylog.entity.LogEntry;
import com.securitylog.repository.LogEntryRepository;
import com.securitylog.util.ZscalerParser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class LogService {

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
        // Strip path separators to prevent directory traversal in the name
        logName = logName.replaceAll("[/\\\\]", "_");

        List<LogEntry> batch = new ArrayList<>();
        int totalSaved = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                LogEntry entry = ZscalerParser.parseLine(line, logName);
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
        return totalSaved;
    }

    public List<String> listLogNames() {
        return logEntryRepository.findDistinctLogNames();
    }

    public List<LogEntry> getEntriesByLogName(String logName) {
        return logEntryRepository.findByLogNameOrderByRecordIdAsc(logName);
    }
}
