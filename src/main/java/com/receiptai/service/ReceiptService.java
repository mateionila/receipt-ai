package com.receiptai.service;

import com.receiptai.dto.ReceiptResponse;
import com.receiptai.dto.gemini.AnalyzedReceiptDto;
import com.receiptai.dto.gemini.ReceiptItemDto;

import com.receiptai.model.Expense;
import com.receiptai.model.Receipt;
import com.receiptai.model.User;
import com.receiptai.repository.ReceiptRepository;
import com.receiptai.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReceiptService {
    private static final Logger log = LoggerFactory.getLogger(ReceiptService.class);

    private final ReceiptRepository receiptRepository;
    private final UserRepository userRepository;
    private final GeminiParser dataExtractor;
    private final VectorSearchService vectorSearchService;

    public ReceiptService(ReceiptRepository receiptRepository,
                          UserRepository userRepository,
                          GeminiParser dataExtractor,
                          VectorSearchService vectorSearchService) {
        this.receiptRepository = receiptRepository;
        this.userRepository = userRepository;
        this.dataExtractor = dataExtractor;
        this.vectorSearchService = vectorSearchService;
    }

    @Transactional
    public Receipt processAndSaveReceipt(Long userId, MultipartFile file) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        File savedFile = saveImageToDisk(file);

        AnalyzedReceiptDto aiResponse = dataExtractor.extractReceiptData(savedFile);
        
        Receipt receipt = new Receipt();
        receipt.setUser(user);
        receipt.setStoreName(aiResponse.getStoreName());
        
        try {
            receipt.setDate(aiResponse.getDate() != null ? LocalDate.parse(aiResponse.getDate()) : LocalDate.now());
        } catch (Exception e) {
            receipt.setDate(LocalDate.now());
        }
        
        receipt.setTotalAmount(aiResponse.getTotalAmount());
        receipt.setImagePath(savedFile.getAbsolutePath());

        List<Expense> expenses = new ArrayList<>();
        if (aiResponse.getItems() != null) {
            for (ReceiptItemDto item : aiResponse.getItems()) {
                Expense expense = new Expense();
                expense.setName(item.getName());
                expense.setPrice(item.getTotalPrice() != null ? java.math.BigDecimal.valueOf(item.getTotalPrice()) : java.math.BigDecimal.ZERO);
                expense.setReceipt(receipt);
                expenses.add(expense);
            }
        }
        receipt.setExpenses(expenses);

        Receipt savedReceipt = receiptRepository.save(receipt);

        List<Expense> expensesToIndex = savedReceipt.getExpenses();
        if (expensesToIndex == null || expensesToIndex.isEmpty()) {
            expensesToIndex = receipt.getExpenses();
            log.warn("Saved receipt returned an empty expense list, using in-memory entities for indexing");
        }

        log.info("Indexing {} receipt items for semantic search", expensesToIndex.size());

        for (Expense e : expensesToIndex) {
            try {
                Long expenseId = e.getId();
                if (expenseId == null) {
                    log.warn("Skipping product '{}' because it does not have a generated id yet", e.getName());
                    continue;
                }

                vectorSearchService.addProductToVectorIndex(
                        expenseId,
                        e.getName(),
                        e.getPrice().doubleValue(),
                        savedReceipt.getStoreName()
                );
            } catch (Exception ex) {
                log.warn("Indexing failed for product '{}'", e.getName(), ex);
            }
        }

        return savedReceipt;
    }

    private File saveImageToDisk(MultipartFile file) throws IOException {
        java.nio.file.Path uploadPath = java.nio.file.Paths.get("uploads").toAbsolutePath();

        if (!java.nio.file.Files.exists(uploadPath)) {
            java.nio.file.Files.createDirectories(uploadPath);
        }

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

        java.nio.file.Path filePath = uploadPath.resolve(fileName);

        file.transferTo(filePath.toFile());

        return filePath.toFile();
    }

    public List<ReceiptResponse> getUserReceipt(Long userId) {
        List<Receipt> receipts = receiptRepository.findByUserId(userId);
        return receipts.stream()
                .map(r -> new ReceiptResponse(
                        r.getId(),
                        r.getStoreName(),
                        r.getDate(),
                        r.getTotalAmount()
                ))
                .collect(Collectors.toList());
    }
}
