package com.sebbsoonsart.backend.controller;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    private final GoogleDriveService driveService;

    public ImagesController(GoogleDriveService driveService) {
        this.driveService = driveService;
    }

    @GetMapping("/api/images")
    @CrossOrigin(origins = "https://sebbsoon.github.io")
    public List<Map<String, String>> getImages() {
        List<Map<String, String>> resp = driveService.fetchImages();
        return resp;
    }

    @GetMapping("api/images/{id}")
    @CrossOrigin(origins = "https://sebbsoon.github.io")
    public ResponseEntity<byte[]> getImage(@PathVariable String id) {
        try {
            // Optionally, you can detect MIME type dynamically from Drive API
            String mimeType = MediaType.IMAGE_JPEG_VALUE; // default to JPEG
            byte[] data = driveService.downloadImage(id, mimeType);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, mimeType)
                    .body(data);

        } catch (IOException | InterruptedException e) {
            return ResponseEntity.status(503).build(); // service unavailable
        }
    }

    @GetMapping("/api/filter")
    @CrossOrigin(origins = "https://sebbsoon.github.io")
    public ResponseEntity<Map<String, Object>> fetchFilter() {
        Map<String, Object> resp = driveService.fetchFilter();
        if (resp == null || resp.isEmpty()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyMap());
        }
        return ResponseEntity.ok(resp);

    }

    @PutMapping("/api/filter")
    @CrossOrigin(origins = "https://sebbsoon.github.io")
    public ResponseEntity<Map<String, Object>> updateFilter(
            @RequestBody Map<String, Object> newFilter) {

        try {
            Map<String, Object> saved = driveService.updateFilter(newFilter);
            return ResponseEntity.ok(saved);
            
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyMap());
        }
    }

}
