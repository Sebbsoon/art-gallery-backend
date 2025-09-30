package com.sebbsoonsart.backend.controller;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.sebbsoonsart.backend.service.GoogleDriveService;

@RestController
public class ImagesController {
    private static final Logger log = LoggerFactory.getLogger(ImagesController.class);

    private final GoogleDriveService driveService;

    public ImagesController(GoogleDriveService driveService) {
        this.driveService = driveService;
    }

    @GetMapping("/api/images")
    @CrossOrigin(origins = "https://sebbsoon.github.io")
    public List<Map<String, String>> getImages() {
        log.info("Received request to fetch image list");
        List<Map<String, String>> resp = driveService.fetchImages();
        log.info("Returning {} images to client", resp.size());
        return resp;
    }

    @GetMapping("api/images/{id}")
    @CrossOrigin(origins = "https://sebbsoon.github.io")
    public ResponseEntity<byte[]> getImage(@PathVariable String id) {
        log.info("Received request to fetch image with ID={}", id);
        try {
            String mimeType = MediaType.IMAGE_JPEG_VALUE;
            byte[] data = driveService.downloadImage(id, mimeType);

            log.info("Successfully fetched image {} ({} bytes)", id, data.length);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, mimeType)
                    .body(data);

        } catch (IOException e) {
            log.error("I/O error while fetching image {}", id, e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Image fetch interrupted for ID={}", id, e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    @GetMapping("/api/filter")
    @CrossOrigin(origins = "https://sebbsoon.github.io")
    public ResponseEntity<Map<String, Object>> fetchFilter() {
        log.info("Received request to fetch filter config");
        Map<String, Object> resp = driveService.fetchFilter();

        if (resp == null || resp.isEmpty()) {
            log.error("Failed to fetch filter: response was null or empty");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyMap());
        }

        log.info("Successfully fetched filter: {}", resp);
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/api/filter")
    @CrossOrigin(origins = "https://sebbsoon.github.io")
    public ResponseEntity<Map<String, Object>> updateFilter(
            @RequestBody Map<String, Object> newFilter) {
        log.info("Received request to update filter: {}", newFilter);
        try {
            Map<String, Object> saved = driveService.updateFilter(newFilter);
            log.info("Successfully updated filter: {}", saved);
            return ResponseEntity.ok(saved);

        } catch (IOException e) {
            log.error("Failed to update filter with payload: {}", newFilter, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyMap());
        }
    }
}
