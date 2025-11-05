package com.sebbsoonsart.backend.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;

import jakarta.annotation.PostConstruct;

@Service
public class GoogleDriveService {

    private static final Logger log = LoggerFactory.getLogger(GoogleDriveService.class);

    @Value("${google.api.key}")
    private String apiKey;

    @Value("${google.folder.id}")
    private String folderId;

    @Value("${google.filter.id}")
    private String filterId;

    @Value("${google.credentials.json}")
    private String googleCredsJson;

    private final ObjectMapper mapper;
    private final HttpClient httpClient;

    private final Map<String, CachedImage> cache = new ConcurrentHashMap<>();
    private final long CACHE_TTL = 1000 * 60 * 10;

    public GoogleDriveService(ObjectMapper mapper) {
        this.mapper = mapper;
        this.httpClient = HttpClient.newHttpClient();

    }

    @PostConstruct
    private void validateConfig() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing required property: google.api.key");
        }
        if (folderId == null || folderId.isBlank()) {
            throw new IllegalStateException("Missing required property: google.folder.id");
        }
    }

    public List<Map<String, String>> fetchImages() {
        List<Map<String, String>> images = new ArrayList<>();
        String url = buildRequestUrl(folderId, apiKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IOException("Google Drive API returned status "
                        + response.statusCode() + ": " + response.body());
            }

            images = parseImageFiles(response.body());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to fetch images from Google Drive", e);
        }

        return images;
    }

    public Map<String, Object> fetchFilter() {
        try {
            String url = "https://www.googleapis.com/drive/v3/files/" + filterId + "?alt=media&key=" + apiKey;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new IOException("Google Drive API returned status "
                        + response.statusCode() + ": " + response.body());
            }
            return mapper.readValue(response.body(), Map.class);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to fetch data from Google Drive", e);
            return Collections.emptyMap();
        }
    }

    public Map<String, Object> updateFilter(Map<String, Object> newFilter) throws IOException {
        String json = mapper.writeValueAsString(newFilter);

        GoogleCredentials credentials = GoogleCredentials.fromStream(
                new ByteArrayInputStream(googleCredsJson.getBytes(StandardCharsets.UTF_8)))
                .createScoped(Collections.singleton("https://www.googleapis.com/auth/drive"));
        credentials.refreshIfExpired();

        String accessToken = credentials.getAccessToken().getTokenValue();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.googleapis.com/upload/drive/v3/files/"
                        + filterId + "?uploadType=media&supportsAllDrives=true"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return newFilter;
            } else {
                throw new IOException("Drive update failed: "
                        + response.statusCode() + " " + response.body());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted updating filter file", e);
        }
    }

    public byte[] downloadImage(String fileId, String mimeType) throws IOException, InterruptedException {
        CachedImage cached = cache.get(fileId);
        if (cached != null && Instant.now().toEpochMilli() - cached.timestamp < CACHE_TTL) {
            return cached.data;
        }

        String url = "https://www.googleapis.com/drive/v3/files/" + fileId + "?alt=media&key=" + apiKey;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 200) {
            throw new IOException(
                    "Failed to fetch file " + fileId + " from Google Drive, status: " + response.statusCode());
        }

        byte[] data = response.body();
        cache.put(fileId, new CachedImage(data));
        return data;
    }

    private String buildRequestUrl(String folderId, String apiKey) {
        String query = String.format("'%s' in parents", folderId);
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        return String.format(
                "https://www.googleapis.com/drive/v3/files?q=%s&fields=files(id,name,mimeType)&key=%s",
                encodedQuery,
                apiKey);
    }

    private List<Map<String, String>> parseImageFiles(String json) throws IOException {
        List<Map<String, String>> images = new ArrayList<>();
        JsonNode root = mapper.readTree(json);
        JsonNode files = root.get("files");

        if (files != null && files.isArray()) {
            for (JsonNode file : files) {
                String mimeType = file.path("mimeType").asText("");
                if (mimeType.startsWith("image/")) {
                    String id = file.path("id").asText();
                    String name = file.path("name").asText();

                    String publicUrl = "https://drive.google.com/uc?id=" + id;

                    images.add(Map.of(
                            "id", id,
                            "name", name,
                            "url", publicUrl));
                }
            }
        }
        return images;
    }

    public java.io.InputStream downloadImageAsStream(String fileId) throws IOException, InterruptedException {
        String url = "https://www.googleapis.com/drive/v3/files/" + fileId + "?alt=media&key=" + apiKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<java.io.InputStream> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofInputStream() // ✅ stream directly
        );

        if (response.statusCode() != 200) {
            throw new IOException(
                    "Failed to fetch file " + fileId + " from Google Drive, status: " + response.statusCode());
        }

        return response.body();
    }

    private static class CachedImage {
        final byte[] data;
        final long timestamp;

        CachedImage(byte[] data) {
            this.data = data;
            this.timestamp = Instant.now().toEpochMilli();
        }
    }
}
