package com.receiptai.dto.basket;

import java.math.BigDecimal;
import java.util.List;

public class StoreBasketResult {
    private String storeName;
    private BigDecimal totalCost;
    private int foundProductCount;
    private int missingProductCount;
    private List<BasketItemDto> items;

    public StoreBasketResult(String storeName, BigDecimal totalCost, int found, int missing, List<BasketItemDto> items) {
        this.storeName = storeName;
        this.totalCost = totalCost;
        this.foundProductCount = found;
        this.missingProductCount = missing;
        this.items = items;
    }

    public String getStoreName() { return storeName; }
    public BigDecimal getTotalCost() { return totalCost; }
    public int getFoundProductCount() { return foundProductCount; }
    public int getMissingProductCount() { return missingProductCount; }
    public List<BasketItemDto> getItems() { return items; }
}