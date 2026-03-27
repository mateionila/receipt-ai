package com.receiptai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ReceptaiApplication {

    public static void main(String[] args) {

        System.setProperty("jna.library.path", "/opt/homebrew/lib");

        SpringApplication.run(ReceptaiApplication.class, args);
    }

}