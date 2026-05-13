package com.securitylog.controller;

import com.securitylog.service.LogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
public class IngestController {

    private final LogService logService;

    public IngestController(LogService logService) {
        this.logService = logService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<?> ingest(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }

        try {
            int count = logService.ingest(file);
            return ResponseEntity.ok(Map.of(
                "message", "Ingested " + count + " log entries from " + file.getOriginalFilename(),
                "count", count
            ));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to read uploaded file: " + e.getMessage()));
        }
    }
}
