package com.wallet.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.code.dto.TxnInitPayload;
import com.wallet.code.dto.UserCreatedPayload;
import com.wallet.service.Model.Wallet;
import com.wallet.service.Repository.WalletRepo;
import com.wallet.service.Service.WalletService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;

@Configuration
public class WalletKafkaConsumerConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(WalletKafkaConsumerConfig.class);

    @Autowired
    private WalletRepo walletRepo;

    @Autowired
    private WalletService walletService;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = "${wallet.usercreated.topic}", groupId = "wallet")
    public void consumeUserCreated(ConsumerRecord<String, String> payload) {
        if (payload == null || payload.value() == null) {
            LOGGER.warn("Received null payload for user created topic; ignoring.");
            return;
        }
        try {
            UserCreatedPayload userCreatedPayload =
                    objectMapper.readValue(payload.value(), UserCreatedPayload.class);

            String requestId = userCreatedPayload.getRequestId();
            if (requestId != null) {
                MDC.put("requestId", requestId);
            }

            try {
                LOGGER.info("read from kafka topic {}", requestId);

                // Idempotency guard to avoid duplicate inserts on re-delivery
                Wallet existing = walletRepo.findByUserId(userCreatedPayload.getUserId());
                if (existing == null) {
                    Wallet wallet = new Wallet();
                    wallet.setUserId(userCreatedPayload.getUserId());
                    wallet.setUserEmail(userCreatedPayload.getUserEmail());
                    wallet.setBalance(100.0);
                    walletRepo.save(wallet);
                } else {
                    LOGGER.info("Wallet already exists for userId={}, skipping create", userCreatedPayload.getUserId());
                }
            } finally {
                MDC.clear();
            }
        } catch (Exception e) {
            // Prevent infinite retry loops on permanently bad messages
            LOGGER.error("Failed to process user-created message, skipping. raw='{}'", payload.value(), e);
            MDC.clear();
            // optionally: send to a dead-letter topic using a DLT-configured error handler
        }
    }

    @KafkaListener(topics = "${wallet.init.topic}", groupId = "wallet")
    public void consumeTxnInit(ConsumerRecord<String, String> payload) {
        if (payload == null || payload.value() == null) {
            LOGGER.warn("Received null payload for txn init topic; ignoring.");
            return;
        }
        try {
            TxnInitPayload txnInitPayload = objectMapper.readValue(payload.value(), TxnInitPayload.class);

            String requestId = txnInitPayload.getRequestId();
            if (requestId != null) {
                MDC.put("requestId", requestId);
            }

            try {
                LOGGER.info("read from kafka topic {}", requestId);
                walletService.walletTxn(txnInitPayload);
            } finally {
                MDC.clear();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to process txn-init message, skipping. raw='{}'", payload.value(), e);
            MDC.clear();
            // avoid throwing to prevent endless retries on poison messages
        }
    }

}
