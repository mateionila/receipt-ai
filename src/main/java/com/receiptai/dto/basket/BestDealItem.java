package com.receiptai.dto.basket;

import java.math.BigDecimal;

public class BestDealItem {
    private Long id;
    private String productName;
    private String storeName;
    private BigDecimal price;

    public BestDealItem() {}

    public BestDealItem(Long id, String productName, String storeName, BigDecimal price) {
        this.id = id;
        this.productName = productName;
        this.storeName = storeName;
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public String getProductName() {
        return productName;
    }

    public String getStoreName() {
        return storeName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getName() {
        return productName;
    }

    public BigDecimal getMinPrice() {
        return price;
    }
}
