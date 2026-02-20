package com.wallet.fraudservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.fraudservice.dto.FraudClassificationResponse;
import com.wallet.fraudservice.dto.Transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FraudAIService {

    private static final Logger log = LoggerFactory.getLogger(FraudAIService.class);

    private static final String SYSTEM_PROMPT = "You are a fraud detection assistant. "
            + "Classify each transaction as FRAUD or SAFE and provide confidence between 0 and 1. "
            + "Respond only as JSON with this exact format: "
            + "{\"classification\":\"FRAUD\"|\"SAFE\",\"confidence\":<number>}";

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ObjectMapper objectMapper;

    public FraudClassificationResponse classifyTransaction(Transaction txn) {
        String userPrompt = buildPrompt(txn);
        try {
            String aiRawResponse = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .content();

            return parseResponse(aiRawResponse);
        } catch (Exception ex) {
            log.warn("AI classification failed for requestId={}. Falling back to SAFE.", txn.getRequestId(), ex);
            return fallbackSafe();
        }
    }

    private String buildPrompt(Transaction txn) {
        return "Analyze the following transaction and classify as FRAUD or SAFE. "
                + "Return only JSON in the required format.\n"
                + "id: " + txn.getId() + "\n"
                + "fromUserId: " + txn.getFromUserId() + "\n"
                + "toUserId: " + txn.getToUserId() + "\n"
                + "amount: " + txn.getAmount() + "\n"
                + "requestId: " + safe(txn.getRequestId());
    }

    private FraudClassificationResponse parseResponse(String aiRawResponse) {
        if (aiRawResponse == null || aiRawResponse.isBlank()) {
            log.warn("AI returned empty response. Falling back to SAFE.");
            return fallbackSafe();
        }

        try {
            JsonNode node = objectMapper.readTree(aiRawResponse);
            String classification = node.path("classification").asText("SAFE").toUpperCase();
            if (!"FRAUD".equals(classification) && !"SAFE".equals(classification)) {
                classification = "SAFE";
            }

            double confidence = node.path("confidence").asDouble(0.0d);
            confidence = Math.max(0.0d, Math.min(1.0d, confidence));

            return new FraudClassificationResponse(classification, confidence);
        } catch (Exception ex) {
            log.warn("Failed to parse AI JSON response: {}. Falling back to SAFE.", aiRawResponse, ex);
            return fallbackSafe();
        }
    }

    private FraudClassificationResponse fallbackSafe() {
        return new FraudClassificationResponse("SAFE", 0.0d);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
