package com.receiptai;

import com.receiptai.dto.gemini.AnalyzedReceiptDto;
import com.receiptai.dto.gemini.ReceiptItemDto;
import com.receiptai.model.Receipt;
import com.receiptai.model.User;
import com.receiptai.repository.UserRepository;
import com.receiptai.service.GeminiParser;
import com.receiptai.service.ReceiptService;
import com.receiptai.service.VectorSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class ReceiptServiceTest {

    @Autowired
    private ReceiptService receiptService;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private GeminiParser geminiParser;

    @MockBean
    private VectorSearchService vectorSearchService;

    @Test
    void testProcessReceiptFlow() throws IOException {
        User testUser = new User();
        testUser.setName("Tester Tesseract");
        testUser.setEmail("test@ocr.com");
        testUser.setPassword("encoded-password");
        testUser.setRole("ROLE_USER");
        testUser = userRepository.save(testUser);
        Long userId = testUser.getId();

        ReceiptItemDto milk = new ReceiptItemDto();
        milk.setName("Lapte");
        milk.setQuantity(1.0);
        milk.setTotalPrice(8.5);

        AnalyzedReceiptDto analyzedReceipt = new AnalyzedReceiptDto();
        analyzedReceipt.setStoreName("Mega Image");
        analyzedReceipt.setDate("2026-03-27");
        analyzedReceipt.setTotalAmount(8.5);
        analyzedReceipt.setItems(List.of(milk));

        when(geminiParser.extractReceiptData(any())).thenReturn(analyzedReceipt);
        doNothing().when(vectorSearchService).addProductToVectorIndex(any(), any(), any(), any());

        String pathLaPoza = "src/test/resources/test_receipt.png";
        FileInputStream inputStream = new FileInputStream(pathLaPoza);

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "test_receipt.png",
                "image/png",
                inputStream
        );

        Receipt savedReceipt = receiptService.processAndSaveReceipt(userId, multipartFile);

        assertNotNull(savedReceipt.getId());
        assertEquals(userId, savedReceipt.getUser().getId());
        assertEquals("Mega Image", savedReceipt.getStoreName());
        assertEquals(LocalDate.parse("2026-03-27"), savedReceipt.getDate());
        assertEquals(8.5, savedReceipt.getTotalAmount());
        assertEquals(1, savedReceipt.getExpenses().size());
        assertEquals("Lapte", savedReceipt.getExpenses().get(0).getName());
        assertEquals(BigDecimal.valueOf(8.5), savedReceipt.getExpenses().get(0).getPrice());
        assertNotNull(savedReceipt.getImagePath());
    }
}
