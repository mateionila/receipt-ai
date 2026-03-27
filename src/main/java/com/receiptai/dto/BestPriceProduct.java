package com.receiptai.dto;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
public class BestPriceProduct {
    private String storeName;
    private BigDecimal minPrice;
    private Double avgPrice;

    public String getStoreName() { return storeName; }
    public BigDecimal getMinPrice() { return minPrice; }
    public Double getAvgPrice() { return avgPrice; }
}
