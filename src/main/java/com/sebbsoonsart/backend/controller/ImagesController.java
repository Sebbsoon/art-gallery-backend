package com.sebbsoonsart.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sebbsoonsart.backend.service.GoogleDriveService;

@RestController
public class ImagesController {

    private final GoogleDriveService driveService;

    public ImagesController(GoogleDriveService driveService) {
        this.driveService = driveService;
    }

    @GetMapping("/api/images")
    @CrossOrigin(origins = "https://sebbsoon.github.io/art-gallery/")

    public List<Map<String, String>> getImages() {

        List<Map<String, String>> resp = driveService.fetchImages();
        return resp;
    }
}
