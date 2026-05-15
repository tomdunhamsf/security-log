package com.securitylog.controller;

import com.securitylog.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
public class IngestController {

    private static final Logger log = LoggerFactory.getLogger(IngestController.class);

    private final LogService logService;

    public IngestController(LogService logService) {
        this.logService = logService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<?> ingest(@RequestParam("file") MultipartFile file) {
        log.info("Ingest request received: name={} size={}", file.getOriginalFilename(), file.getSize());

        if (file.isEmpty()) {
            log.warn("Rejected empty file upload");
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }

        try {
            int count = logService.ingest(file);
            log.info("Ingested {} entries from {}", count, file.getOriginalFilename());
            return ResponseEntity.ok(Map.of(
                "message", "Ingested " + count + " log entries from " + file.getOriginalFilename(),
                "count", count
            ));
        } catch (Exception e) {
            log.error("Ingest failed for file {}: {}", file.getOriginalFilename(), e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Ingest failed: " + e.getMessage()));
        }
    }
}
