package com.dietagent.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
public class PexelsService {

    private static final String API_URL = "https://api.pexels.com/v1/search";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public PexelsService(RestTemplate restTemplate, @Value("${pexels.api-key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey;
    }

    public List<PexelsImage> searchImages(String query, int perPage) {
        if (perPage <= 0) perPage = 5;
        if (perPage > 15) perPage = 15;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", apiKey);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
            String url = API_URL + "?query=" + encodedQuery + "&per_page=" + perPage;
            log.debug("Pexels API URL: {}", url);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            List<PexelsImage> images = parseResponse(response.getBody());
            // 只记数量不落响应体全文：Pexels 响应含全部图片 URL，全量打日志既刷屏又泄露配置上下文
            log.info("Pexels 搜索 '{}' 返回 {} 张图片", query, images.size());
            return images;
        } catch (Exception e) {
            log.error("Pexels API 调用失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<PexelsImage> parseResponse(String json) {
        List<PexelsImage> images = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode photos = root.get("photos");
            if (photos != null && photos.isArray()) {
                for (JsonNode photo : photos) {
                    PexelsImage img = new PexelsImage();
                    img.setId(photo.get("id").asLong());
                    img.setPhotographer(photo.get("photographer").asText());
                    img.setPhotographerUrl(photo.get("photographer_url").asText());
                    img.setAlt(photo.has("alt") && !photo.get("alt").isNull() ? photo.get("alt").asText() : "");

                    JsonNode src = photo.get("src");
                    if (src != null) {
                        img.setOriginalUrl(getTextOrEmpty(src, "original"));
                        img.setLargeUrl(getTextOrEmpty(src, "large"));
                        img.setMediumUrl(getTextOrEmpty(src, "medium"));
                        img.setSmallUrl(getTextOrEmpty(src, "small"));
                        img.setTinyUrl(getTextOrEmpty(src, "tiny"));
                    }
                    images.add(img);
                }
            }
        } catch (Exception e) {
            log.error("解析 Pexels 响应失败: {}", e.getMessage());
        }
        return images;
    }

    private String getTextOrEmpty(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        return (fieldNode != null && !fieldNode.isNull()) ? fieldNode.asText() : "";
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PexelsImage {
        private long id;
        private String photographer;
        private String photographerUrl;
        private String alt;
        private String originalUrl;
        private String largeUrl;
        private String mediumUrl;
        private String smallUrl;
        private String tinyUrl;
    }
}
