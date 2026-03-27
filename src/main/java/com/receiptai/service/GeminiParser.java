package com.receiptai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.receiptai.dto.gemini.AnalyzedReceiptDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

@Service
public class GeminiParser {
    private static final Logger log = LoggerFactory.getLogger(GeminiParser.class);
    
    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiParser() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public AnalyzedReceiptDto extractReceiptData(File imageFile) {
        try {
            byte[] fileContent = Files.readAllBytes(imageFile.toPath());
            String encodedImage = Base64.getEncoder().encodeToString(fileContent);

            String prompt = """
                    Look at this receipt image.
                    1. Extract the merchant name, total amount, and date.
                    2. EXTRACT THE LIST OF ITEMS purchased.
                
                    Return ONLY a raw JSON object (no markdown) with this structure:
                    {
                        "storeName": "string",
                        "totalAmount": number,
                        "date": "YYYY-MM-DD",
                        "category": "string",
                        "items": [ { "name": "string", "quantity": number, "totalPrice": number } ]
                    }
                    """;

            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", prompt);

            Map<String, Object> imagePart = new HashMap<>();
            Map<String, Object> inlineData = new HashMap<>();
            inlineData.put("mime_type", "image/jpeg");
            inlineData.put("data", encodedImage);
            imagePart.put("inline_data", inlineData);

            return callGeminiApi(List.of(textPart, imagePart));

        } catch (Exception e) {
            throw new RuntimeException("Gemini Vision failed: " + e.getMessage(), e);
        }
    }

    private AnalyzedReceiptDto callGeminiApi(List<Map<String, Object>> parts) {
        String finalUrl = apiUrl + "?key=" + apiKey;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", parts);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(content));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(finalUrl, entity, JsonNode.class);
            JsonNode responseBody = response.getBody();
            if (responseBody == null || !responseBody.has("candidates")) {
                throw new RuntimeException("Raspuns invalid Gemini: " + responseBody);
            }

            JsonNode candidates = responseBody.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                throw new RuntimeException("Gemini nu a returnat niciun rezultat.");
            }

            JsonNode textNode = candidates.get(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text");

            if (textNode.isMissingNode() || textNode.isNull()) {
                throw new RuntimeException("Raspuns Gemini fara camp text.");
            }

            String jsonResultString = textNode.asText();
            jsonResultString = jsonResultString.replace("```json", "").replace("```", "").trim();

            return objectMapper.readValue(jsonResultString, AnalyzedReceiptDto.class);

        } catch (Exception e) {
            log.error("Gemini request failed", e);
            throw new RuntimeException("Eroare comunicare Gemini: " + e.getMessage(), e);
        }
    }
}
