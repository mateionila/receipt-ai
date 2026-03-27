package com.receiptai.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name  = "expenses", indexes = {
        @Index(name = "idx_expense_name", columnList = "name"),
        @Index(name = "idx_expense_price", columnList = "price")
})
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private BigDecimal price;
    private BigDecimal quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id")
    private Receipt receipt;

    public Expense(){}

    public String getStoreName() {
        return receipt.getStoreName();
    }
}
