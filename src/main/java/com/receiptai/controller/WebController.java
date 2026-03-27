package com.receiptai.controller;

import com.receiptai.dto.ReceiptResponse;
import com.receiptai.dto.basket.BestDealItem;
import com.receiptai.dto.basket.StoreBasketResult;
import com.receiptai.model.Receipt;
import com.receiptai.model.User;
import com.receiptai.repository.ExpenseRepository;
import com.receiptai.repository.UserRepository;
import com.receiptai.service.ReceiptService;
import com.receiptai.service.SmartBasketService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Controller
public class WebController {
    private final ReceiptService receiptService;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final SmartBasketService smartBasketService;

    public WebController(ReceiptService receiptService, ExpenseRepository expenseRepository, UserRepository userRepository, SmartBasketService smartBasketService) {
        this.receiptService = receiptService;
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.smartBasketService = smartBasketService;
    }

    @GetMapping("/scan")
    public String showScanPage(){
        return "scan";
    }

    @PostMapping("/scan")
    public String handleUpload(@RequestParam("file") MultipartFile file, @AuthenticationPrincipal UserDetails userDetails, Model model){
        try{
            User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
            Receipt receipt = receiptService.processAndSaveReceipt(user.getId(),file);

            model.addAttribute("receipt", receipt);
            model.addAttribute("succes", "Bon procesat cu succes! Total: " + receipt.getTotal());
        }catch(Exception e){
            model.addAttribute("error", "Eroare: " + e.getMessage());
        }
        return "scan";
    }

    @GetMapping("/history")
    public String showHistoryPage(@AuthenticationPrincipal UserDetails userDetails, Model model){
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        List<ReceiptResponse> receipts = receiptService.getUserReceipt(user.getId());
        model.addAttribute("receipts", receipts);
        return "history";
    }

    @GetMapping("/smart-basket")
    public String showSmartBasketPage(){
        return "smart-basket";
    }

    @PostMapping("/smart-basket/calculate")
    public String calculateBasket(@RequestParam("products") String productsString,
                                  @RequestParam("strategy") String strategy, Model model){
        List<String> shoppingList = Arrays.stream(productsString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if(shoppingList.isEmpty()){
            model.addAttribute("error", "Nu ai introdus niciun produs!");
            return "smart-basket";
        }

        if("single".equals(strategy)){
            List<StoreBasketResult> results = smartBasketService.cheapestStore(shoppingList);
            model.addAttribute("singleStoreResults", results);
        }
        else{
            Map<String, List<BestDealItem>> results = smartBasketService.calculateBestMixedBasket(shoppingList);
            model.addAttribute("mixedStoreResults", results);

            BigDecimal totalAbsolut = results.values().stream()
                    .flatMap(List::stream)
                    .map(BestDealItem::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            model.addAttribute("totalAbsolut", totalAbsolut);
        }
        model.addAttribute("lastList", productsString);
        return "smart-basket";
    }
}
