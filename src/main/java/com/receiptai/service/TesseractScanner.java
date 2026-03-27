package com.receiptai.service;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;
import java.io.File;

@Service
public class TesseractScanner implements ReceiptScanner {

    private final ITesseract tesseract;

    public TesseractScanner(ITesseract tesseract) {
        this.tesseract = tesseract;
    }

    @Override
    public String extractText(File imageFile){
        try{
            return tesseract.doOCR(imageFile);
        }
        catch (TesseractException e)
        {
            throw new RuntimeException("Eroare la tesseract!");
        }
    }

}
