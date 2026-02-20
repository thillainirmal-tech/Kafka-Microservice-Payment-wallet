package com.wallet.fraudservice.controller;

import com.wallet.fraudservice.dto.FraudAiResponse;
import com.wallet.fraudservice.dto.FraudClassificationResponse;
import com.wallet.fraudservice.dto.Transaction;
import com.wallet.fraudservice.service.FraudAIService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fraud")
public class FraudController {

    @Autowired
    private FraudAIService fraudAIService;

    @PostMapping("/analyze")
    public FraudAiResponse analyze(@RequestBody Transaction transaction) {
        FraudClassificationResponse response = fraudAIService.classifyTransaction(transaction);
        return new FraudAiResponse(response.getClassification(), response.getConfidence());
    }
}
