package com.receiptai.dto;

import com.receiptai.dto.gemini.ProductDto;

import java.time.LocalDate;
import java.util.List;

public class ReceiptResponse {
    private Long id;
    private String merchantName;
    private Double total;
    private LocalDate date;
    private String rawTextSnippet;
    private List<ProductDto> items;

    public ReceiptResponse(Long id, String merchantName, LocalDate date, Double total) {
        this.id = id;
        this.merchantName = merchantName;
        this.date = date;
        this.total = total;
    }

    public ReceiptResponse(Long id, String merchantName, Double total, LocalDate date, String rawText, List<ProductDto> items) {
        this.id = id;
        this.merchantName = merchantName;
        this.total = total;
        this.date = date;
        this.rawTextSnippet = rawText;
        this.items = items;
    }
    
    public Long getId() { return id; }
    public String getMerchantName() { return merchantName; }
    public Double getTotal() { return total; }
    public LocalDate getDate() { return date; }
    public String getRawTextSnippet() { return rawTextSnippet; }
    public List<ProductDto> getItems() { return items; }
}
