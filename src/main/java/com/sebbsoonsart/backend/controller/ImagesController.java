package com.example.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
public class ImagesController {

    private final GoogleDriveService driveService = new GoogleDriveService();

    @GetMapping("/api/images")
    public List<Map<String, String>> getImages() {
        return driveService.fetchImages();
    }
}
