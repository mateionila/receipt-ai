package com.receiptai.scheduler;

import com.receiptai.dto.basket.BestDealItem;
import com.receiptai.repository.ExpenseRepository;
import com.receiptai.service.VectorSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(prefix = "app.sync", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DataSyncScheduler {
    private static final Logger log = LoggerFactory.getLogger(DataSyncScheduler.class);

    private final ExpenseRepository expenseRepository;
    private final VectorSearchService vectorSearchService;

    public DataSyncScheduler(ExpenseRepository expenseRepository, VectorSearchService vectorSearchService) {
        this.expenseRepository = expenseRepository;
        this.vectorSearchService = vectorSearchService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("Application started, rebuilding the vector master index");
        rebuildMasterIndex();
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional(readOnly = true)
    public void rebuildMasterIndex() {
        log.info("Starting vector master index rebuild");

        LocalDate pastDate = LocalDate.now().minusDays(30);
        List<BestDealItem> products = expenseRepository.findProductsSince(pastDate);

        if (products.isEmpty()) {
            log.info("Skipping rebuild because no recent products were found in SQL");
            return;
        }

        log.info("Found {} products for vector rebuild", products.size());

        List<String> productNames = products.stream()
                .map(BestDealItem::getProductName)
                .collect(Collectors.toList());

        List<List<Float>> vectors = new ArrayList<>();
        int batchSize = 50;

        for (int i = 0; i < productNames.size(); i += batchSize) {
            int end = Math.min(productNames.size(), i + batchSize);
            List<String> batch = productNames.subList(i, end);

            try {
                List<List<Float>> batchVectors = vectorSearchService.getEmbeddingsBatch(batch);
                if (batchVectors != null) {
                    vectors.addAll(batchVectors);
                }
            } catch (Exception e) {
                log.error("Failed to vectorize batch {}-{}", i, end, e);
            }
        }

        if (vectors.size() != products.size()) {
            log.error("Rebuild aborted because vector count {} does not match product count {}", vectors.size(), products.size());
            return;
        }

        List<Map<String, Object>> itemsPayload = new ArrayList<>();
        for (int i = 0; i < products.size(); i++) {
            BestDealItem p = products.get(i);
            List<Float> vec = vectors.get(i);

            Map<String, Object> itemData = new HashMap<>();
            itemData.put("id", p.getId());
            itemData.put("vector", vec);
            itemData.put("price", p.getPrice());
            itemData.put("name", p.getProductName());

            String store = p.getStoreName();
            itemData.put("store", store != null ? store : "Unknown Store");

            itemsPayload.add(itemData);
        }

        vectorSearchService.rebuildMaster(itemsPayload);

        log.info("Vector master index rebuild completed successfully");
    }
}
