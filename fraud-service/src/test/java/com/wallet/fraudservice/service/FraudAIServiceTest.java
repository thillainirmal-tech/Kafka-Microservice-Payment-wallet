package com.wallet.fraudservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.fraudservice.dto.FraudClassificationResponse;
import com.wallet.fraudservice.dto.Transaction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudAIServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private FraudAIService fraudAIService;

    @Test
    void classifyTransaction_shouldReturnSafe() throws Exception {
        Transaction txn = buildTxn();

        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("{\"classification\":\"SAFE\",\"confidence\":0.81}");
        when(objectMapper.readTree("{\"classification\":\"SAFE\",\"confidence\":0.81}"))
                .thenReturn(new ObjectMapper().readTree("{\"classification\":\"SAFE\",\"confidence\":0.81}"));

        FraudClassificationResponse result = fraudAIService.classifyTransaction(txn);

        Assertions.assertEquals("SAFE", result.getClassification());
        Assertions.assertEquals(0.81d, result.getConfidence(), 0.0001);
    }

    @Test
    void classifyTransaction_shouldReturnFraud() throws Exception {
        Transaction txn = buildTxn();

        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("{\"classification\":\"FRAUD\",\"confidence\":0.93}");
        when(objectMapper.readTree("{\"classification\":\"FRAUD\",\"confidence\":0.93}"))
                .thenReturn(new ObjectMapper().readTree("{\"classification\":\"FRAUD\",\"confidence\":0.93}"));

        FraudClassificationResponse result = fraudAIService.classifyTransaction(txn);

        Assertions.assertEquals("FRAUD", result.getClassification());
        Assertions.assertEquals(0.93d, result.getConfidence(), 0.0001);
    }

    @Test
    void classifyTransaction_shouldFallbackToSafeWhenInvalidJson() throws Exception {
        Transaction txn = buildTxn();

        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("invalid-json");
        when(objectMapper.readTree("invalid-json"))
                .thenThrow(new RuntimeException("cannot parse"));

        FraudClassificationResponse result = fraudAIService.classifyTransaction(txn);

        Assertions.assertEquals("SAFE", result.getClassification());
        Assertions.assertEquals(0.0d, result.getConfidence(), 0.0001);
    }

    private Transaction buildTxn() {
        Transaction txn = new Transaction();
        txn.setId(10L);
        txn.setFromUserId(100L);
        txn.setToUserId(200L);
        txn.setAmount(50.0d);
        txn.setRequestId("REQ-10");
        return txn;
    }
}
