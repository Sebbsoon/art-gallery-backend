package com.example.backend.service;

import java.util.*;

public class GoogleDriveService {

    private final String API_KEY = System.getenv("GOOGLE_API_KEY");
    private final String FOLDER_ID = System.getenv("GOOGLE_FOLDER_ID");

    public List<Map<String, String>> fetchImages() {
        List<Map<String, String>> images = new ArrayList<>();

        // TODO: call Google Drive API with API_KEY + FOLDER_ID
        // Example:
        // for each file: if mimeType starts with "image/"
        // add { "url": "https://drive.google.com/uc?export=view&id=FILE_ID", "name": "filename" }

        return images;
    }
}
