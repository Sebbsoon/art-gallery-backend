package com.sebbsoonsart.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

        } catch (Exception e) {
            return ResponseEntity.status(503).build(); // service unavailable
        }
    }
}
