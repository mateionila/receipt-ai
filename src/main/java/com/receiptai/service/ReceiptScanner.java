package com.receiptai.service;


import java.io.File;

public interface ReceiptScanner {
    String extractText(File imageFile);
}
