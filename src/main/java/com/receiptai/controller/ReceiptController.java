package com.receiptai.controller;

import com.receiptai.dto.ReceiptResponse;
import com.receiptai.dto.gemini.ProductDto;
import com.receiptai.model.Receipt;
import com.receiptai.service.ReceiptService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {
    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService){
        this.receiptService = receiptService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ReceiptResponse> uploadReceipt(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId
    ) throws IOException {
        if(file.isEmpty()){
            throw new RuntimeException("Fisier gol!");
        }
        Receipt processedReceipt = receiptService.processAndSaveReceipt(userId, file);
        
        List<ProductDto> itemsDto = processedReceipt.getExpenses().stream().map(
                e -> new ProductDto(
                        e.getName(),
                        e.getPrice() != null ? e.getPrice().doubleValue() : 0.0,
                        e.getQuantity() != null ? e.getQuantity().doubleValue() : 1.0
                )
        ).toList();
        
        String rawText = processedReceipt.getRawOcrText();

        ReceiptResponse response = new ReceiptResponse(
                        processedReceipt.getId(),
                        processedReceipt.getStoreName(),
                        processedReceipt.getTotalAmount(), 
                        processedReceipt.getDate(),
                        rawText,
                        itemsDto
                );
        return ResponseEntity.ok(response);
    }
}
