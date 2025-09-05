package com.sebbsoonsart.backend.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GoogleDriveService {

    private final String API_KEY = System.getenv("GOOGLE_API_KEY");
    private final String FOLDER_ID = System.getenv("GOOGLE_FOLDER_ID");
    private final ObjectMapper mapper = new ObjectMapper();

    public List<Map<String, String>> fetchImages() {
        List<Map<String, String>> images = new ArrayList<>();
        try {
            HttpClient client = HttpClient.newHttpClient();
            String url = String.format(
                    "https://www.googleapis.com/drive/v3/files?q='%s'+in+parents&fields=files(id,name,mimeType)&key=%s",
                    FOLDER_ID,
                    API_KEY
            );

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
                        img.put("url", "https://drive.google.com/uc?export=view&id=" + file.get("id").asText());
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
