package com.receiptai.controller;


import com.receiptai.dto.BestPriceProduct;
import com.receiptai.repository.ExpenseRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/statistics")
public class StatisticController {
    private final ExpenseRepository expenseRepository;

    public StatisticController(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    @GetMapping("/price-check")
    public List<BestPriceProduct> checkPrice(@RequestParam String product){
        return expenseRepository.findBestPricesForProduct(product, LocalDate.now().minusDays(3));
    }
}
