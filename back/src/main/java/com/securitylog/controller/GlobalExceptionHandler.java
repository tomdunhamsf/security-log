package com.securitylog.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxSize(MaxUploadSizeExceededException e) {
        log.warn("Upload rejected — file too large: {}", e.getMessage());
        return ResponseEntity.status(413).body(Map.of("error", "File exceeds the maximum allowed size."));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, String>> handleMultipart(MultipartException e, HttpServletRequest req) {
        log.error("Multipart error on {} {}: {}", req.getMethod(), req.getRequestURI(), e.getMessage(), e);
        return ResponseEntity.badRequest().body(Map.of("error", "Multipart error: " + e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAll(Exception e, HttpServletRequest req) {
        log.error("Unhandled exception on {} {}: {}", req.getMethod(), req.getRequestURI(), e.getMessage(), e);
        return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getClass().getSimpleName() + ": " + e.getMessage()));
    }
}
