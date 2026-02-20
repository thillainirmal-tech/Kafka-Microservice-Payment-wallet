package com.wallet.fraudservice.kafka;

import com.wallet.fraudservice.dto.FraudClassificationResponse;
import com.wallet.fraudservice.dto.FraudResult;
import com.wallet.fraudservice.dto.Transaction;
import com.wallet.fraudservice.service.FraudAIService;
import com.wallet.fraudservice.service.FraudResultProducer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionFraudListener {

    private static final Logger log = LoggerFactory.getLogger(TransactionFraudListener.class);

    @Autowired
    private FraudAIService fraudAIService;

    @Autowired
    private FraudResultProducer fraudResultProducer;

    @KafkaListener(topics = "transactions", groupId = "fraud-service", containerFactory = "kafkaListenerContainerFactory")
    public void onTransaction(Transaction txn) {
        if (txn == null) {
            log.warn("Received null transaction payload; skipping fraud check.");
            return;
        }

        FraudClassificationResponse response = fraudAIService.classifyTransaction(txn);

        FraudResult fraudResult = new FraudResult();
        fraudResult.setId(txn.getId());
        fraudResult.setRequestId(txn.getRequestId());
        fraudResult.setClassification(response.getClassification());
        fraudResult.setFraud("FRAUD".equalsIgnoreCase(response.getClassification()));
        fraudResult.setConfidence(response.getConfidence());

        fraudResultProducer.publish(fraudResult);
        log.info("Processed fraud classification for requestId={} classification={} confidence={}",
                txn.getRequestId(), response.getClassification(), response.getConfidence());
    }
}
