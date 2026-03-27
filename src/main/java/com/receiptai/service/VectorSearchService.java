package com.receiptai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class VectorSearchService {
    private static final Logger log = LoggerFactory.getLogger(VectorSearchService.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${services.embedding.single-url}")
    private String pythonEmbedUrl;

    @Value("${services.embedding.batch-url}")
    private String pythonBatchUrl;

    @Value("${services.vector.search-url}")
    private String cppSearchUrl;

    @Value("${services.vector.add-url}")
    private String cppAddUrl;

    @Value("${services.vector.rebuild-url}")
    private String cppRebuildUrl;

    private record PyRequest(String text) {}
    private record PyResponse(List<Float> vector, int dim, double time_ms) {}

    private record PyBatchRequest(List<String> texts) {}
    private record PyBatchResponse(int count, List<List<Float>> vectors, int dim, double time_ms) {}

    private record CppSearchRequest(List<Float> vector, int k) {}

    public static class SearchResult {
        public long id;
        public float distance;
        public String source;
        public String name;
        public float price;
        public String store;
        
        public SearchResult() {}
    }

    public static class CppResponse { public List<SearchResult> results; }

    private record CppAddRequest(long id, List<Float> vector, float price, String name, String store) {}

    public List<SearchResult> searchSimilarProducts(String queryText, int k) {
        PyResponse pyRes = restTemplate.postForObject(pythonEmbedUrl, new PyRequest(queryText), PyResponse.class);
        if (pyRes != null && pyRes.vector() != null && !pyRes.vector().isEmpty()) {
            log.debug("Embedding generated for query '{}'", queryText);
        } else {
            log.warn("Embedding service returned an empty vector for query '{}'", queryText);
        }
        if (pyRes == null || pyRes.vector() == null) return Collections.emptyList();

        CppResponse cppRes = restTemplate.postForObject(cppSearchUrl, new CppSearchRequest(pyRes.vector(), k), CppResponse.class);
        return cppRes != null ? cppRes.results : Collections.emptyList();
    }

    public List<List<Float>> getEmbeddingsBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) return Collections.emptyList();

        try {
            PyBatchResponse response = restTemplate.postForObject(
                pythonBatchUrl,
                new PyBatchRequest(texts), 
                PyBatchResponse.class
            );
            
            if (response != null && response.vectors() != null) {
                return response.vectors();
            }
        } catch (Exception e) {
            log.error("Failed to generate batch embeddings", e);
        }
        return Collections.emptyList();
    }

    public void addProductToVectorIndex(Long id, String name, Double price, String storeName) {
        PyResponse pyRes = null;

        try {
            pyRes = restTemplate.postForObject(pythonEmbedUrl, new PyRequest(name), PyResponse.class);
        } catch (Exception e) {
            log.error("Could not connect to the embedding service at {}", pythonEmbedUrl, e);
            return;
        }

        if (pyRes == null || pyRes.vector() == null) {
            log.warn("Embedding service returned null for product '{}'", name);
            return;
        }

        try {
            restTemplate.postForObject(cppAddUrl, new CppAddRequest(id, pyRes.vector(), price.floatValue(), name, storeName), String.class);
        } catch (Exception e) {
            log.error("Could not connect to the vector index service at {}", cppAddUrl, e);
        }
    }

    public void rebuildMaster(List<Map<String, Object>> items) {
        try {
            Map<String, Object> payload = Map.of("items", items);
            restTemplate.postForObject(cppRebuildUrl, payload, String.class);
        } catch (Exception e) {
            log.error("Failed to rebuild vector master index via {}", cppRebuildUrl, e);
        }
    }
}
