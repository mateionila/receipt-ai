package com.receiptai.service;

import com.receiptai.dto.basket.BasketItemDto;
import com.receiptai.dto.basket.BestDealItem;
import com.receiptai.dto.basket.StoreBasketResult;
import com.receiptai.repository.ExpenseRepository;
import com.receiptai.service.VectorSearchService.SearchResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SmartBasketService {

    private final VectorSearchService vectorSearchService;
    private final ExpenseRepository expenseRepository;

    public SmartBasketService(VectorSearchService vectorSearchService, ExpenseRepository expenseRepository) {
        this.vectorSearchService = vectorSearchService;
        this.expenseRepository = expenseRepository;
    }

    private void applyTextMatchBoost(List<SearchResult> candidates, String query) {
        String queryLower = query.toLowerCase();
        for (SearchResult c : candidates) {
            if (c.name != null && c.name.toLowerCase().contains(queryLower)) {
                c.distance -= 1.0f;
            }
        }
        candidates.sort(Comparator.comparingDouble(c -> c.distance));
    }

    public Map<String, List<BestDealItem>> calculateBestMixedBasket(List<String> shoppingList) {
        Map<String, List<BestDealItem>> basketByStore = new ConcurrentHashMap<>();

        shoppingList.parallelStream().forEach(itemCautat -> {
            List<SearchResult> candidates = vectorSearchService.searchSimilarProducts(itemCautat, 10);

            if (candidates != null && !candidates.isEmpty()) {
                applyTextMatchBoost(candidates, itemCautat);

                SearchResult bestMatch = candidates.stream()
                        .filter(c -> c.distance < 1.5f)
                        .min(Comparator.comparingDouble(c -> c.distance))
                        .orElse(null);

                if (bestMatch != null) {
                    String storeName = bestMatch.store;
                    if (storeName == null || storeName.isEmpty() || "Unknown".equals(storeName)) {
                        storeName = expenseRepository.findStoreNameByExpenseId(bestMatch.id);
                    }

                    if (storeName != null) {
                        BestDealItem item = new BestDealItem(
                                bestMatch.id,
                                bestMatch.name,
                                storeName,
                                BigDecimal.valueOf(bestMatch.price)
                        );
                        basketByStore.compute(storeName, (k, v) -> {
                            if (v == null) v = new ArrayList<>();
                            v.add(item);
                            return v;
                        });
                    }
                }
            }
        });

        return basketByStore;
    }

    public List<StoreBasketResult> cheapestStore(List<String> shoppingList) {
        Map<String, List<BasketItemDto>> potentialBaskets = new ConcurrentHashMap<>();

        shoppingList.parallelStream().forEach(itemCautat -> {
            List<SearchResult> candidates = vectorSearchService.searchSimilarProducts(itemCautat, 10);

            if (candidates != null && !candidates.isEmpty()) {
                applyTextMatchBoost(candidates, itemCautat);

                SearchResult bestMatch = candidates.stream()
                        .filter(c -> c.distance < 1.5f)
                        .min(Comparator.comparingDouble(c -> c.distance))
                        .orElse(null);

                if (bestMatch != null) {
                    String storeName = bestMatch.store;
                    if (storeName == null || storeName.isEmpty() || "Unknown".equals(storeName)) {
                        storeName = expenseRepository.findStoreNameByExpenseId(bestMatch.id);
                    }

                    if (storeName != null) {
                        BigDecimal price = BigDecimal.valueOf(bestMatch.price);
                        BasketItemDto basketItem = new BasketItemDto(bestMatch.name, price);

                        potentialBaskets.compute(storeName, (k, v) -> {
                            if (v == null) v = new ArrayList<>();
                            v.add(basketItem);
                            return v;
                        });
                    }
                }
            }
        });

        List<StoreBasketResult> finalResults = new ArrayList<>();
        for (Map.Entry<String, List<BasketItemDto>> entry : potentialBaskets.entrySet()) {
            String store = entry.getKey();
            List<BasketItemDto> foundItems = entry.getValue();
            BigDecimal total = foundItems.stream().map(BasketItemDto::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
            int uniqueFoundCount = foundItems.size();
            int missingCount = Math.max(0, shoppingList.size() - uniqueFoundCount);
            finalResults.add(new StoreBasketResult(store, total, uniqueFoundCount, missingCount, foundItems));
        }
        finalResults.sort(Comparator.comparingInt(StoreBasketResult::getMissingProductCount).thenComparing(StoreBasketResult::getTotalCost));
        return finalResults;
    }
}
