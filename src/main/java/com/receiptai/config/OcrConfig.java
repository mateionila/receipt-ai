package com.receiptai.config;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.File;

@Configuration
public class OcrConfig {

    @Bean
    public ITesseract tesseract() {
        Tesseract tesseract = new Tesseract();

        File tessDataFolder = new File("src/main/resources/tessdata");
        if (!tessDataFolder.exists()) {
            throw new RuntimeException("Folderul tessdata nu a e: " + tessDataFolder.getAbsolutePath());
        }
        tesseract.setDatapath(tessDataFolder.getAbsolutePath());

        tesseract.setLanguage("ron");

        return tesseract;
    }
}