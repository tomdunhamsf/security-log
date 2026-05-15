package com.securitylog.controller;

import com.securitylog.dto.AnalysisResult;
import com.securitylog.entity.LogEntry;
import com.securitylog.service.LogService;
import com.securitylog.service.OpenAiAnalyzer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/display")
public class DisplayController {

    private final LogService logService;
    private final OpenAiAnalyzer openAiAnalyzer;

    public DisplayController(LogService logService , OpenAiAnalyzer openAiAnalyzer) {
        this.logService = logService;
        this.openAiAnalyzer = openAiAnalyzer;
    }

    /** Returns a JSON array of all distinct log file names. */
    @GetMapping
    public ResponseEntity<List<String>> listLogs() {
        return ResponseEntity.ok(logService.listLogNames());
    }

    /** Returns a JSON array of all log entries for the given log name. */
    @GetMapping("/{logName}")
    public ResponseEntity<?> getLog(@PathVariable String logName) {
        List<LogEntry> entries = logService.getEntriesByLogName(logName);
        if (entries.isEmpty()) {
            return ResponseEntity.status(404)
                    .body(Map.of("error", "No entries found for log: " + logName));
        }
        return ResponseEntity.ok(entries);
    }
 
    /*  Sends all rows for the log to OpenAI and returns a threat analysis. */
    @PostMapping("/{logName}/analyze")
    public ResponseEntity<?> analyzeLog(@PathVariable String logName) {

        List<LogEntry> entries = logService.getEntriesByLogName(logName);
        if (entries.isEmpty()) {
            return ResponseEntity.status(404)
                    .body(Map.of("error", "No entries found for log: " + logName));
        }
        try {
            AnalysisResult result = openAiAnalyzer.analyze(entries);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(502)
                    .body(Map.of("error", "Analysis failed: " + e.getMessage()));
        }
    }
    
}
