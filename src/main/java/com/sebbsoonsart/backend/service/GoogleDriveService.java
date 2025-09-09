package com.sebbsoonsart.backend.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GoogleDriveService {

    @Value("${google.api.key}")
    private String apiKey;

    @Value("${google.folder.id}")
    private String folderId;

    private final ObjectMapper mapper = new ObjectMapper();

    public List<Map<String, String>> fetchImages() {

        List<Map<String, String>> images = new ArrayList<>();

        try {
            if (apiKey == null || folderId == null) {
                throw new IllegalStateException("GOOGLE_API_KEY or GOOGLE_FOLDER_ID not set!");
            }

            HttpClient client = HttpClient.newHttpClient();
            String query = String.format("'%s' in parents", folderId);
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

            String url = String.format(
                    "https://www.googleapis.com/drive/v3/files?q=%s&fields=files(id,name,mimeType)&key=%s",
                    encodedQuery,
                    apiKey);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JsonNode root = mapper.readTree(response.body());
            JsonNode files = root.get("files");

            if (files != null && files.isArray()) {
                for (JsonNode file : files) {
                    String mimeType = file.get("mimeType").asText();
                    if (mimeType.startsWith("image/")) {
                        Map<String, String> img = new HashMap<>();
                        img.put("id", file.get("id").asText());
                        img.put("name", file.get("name").asText());

                        String signedUrl = String.format(
                                "https://www.googleapis.com/drive/v3/files/%s?alt=media&key=%s",
                                file.get("id").asText(),
                                apiKey);

                        img.put("url", signedUrl);
                        images.add(img);
                    }
                }
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return images;
    }
}
