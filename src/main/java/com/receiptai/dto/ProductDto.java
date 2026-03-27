package com.receiptai.dto.gemini;

public class ProductDto {
    public String name;
    public Double price;
    public Double qty;

    public ProductDto() {}

    public ProductDto(String name, Double price, Double qty) {
        this.name = name;
        this.price = price;
        this.qty = qty;
    }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    
    public Double getQty() { return qty; }
    public void setQty(Double qty) { this.qty = qty; }

    public String name() { return name; }
    public Double price() { return price; }
    public Double quantity() { return qty; }
}
