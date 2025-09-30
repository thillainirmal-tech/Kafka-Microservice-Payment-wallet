package com.wallet.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.code.dto.TxnCompletedPayload;
import com.wallet.service.Service.WalletService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PgTxnConsumer {
    private static final Logger log = LoggerFactory.getLogger(PgTxnConsumer.class);

    private final ObjectMapper objectMapper;
    private final WalletService walletService;

    @Value("${wallet.completed.topic}") private String topic;

    @KafkaListener(topics = "${wallet.completed.topic}", groupId = "wallet")
    public void onCompleted(String raw) {
        try {
            var e = objectMapper.readValue(raw, TxnCompletedPayload.class);
            if (Boolean.TRUE.equals(e.getSuccess())) {
                walletService.applyPgCapture(e);   // idempotent credit/debit by txnId
            } else {
                log.info("Received failed event for txnId={}, reason={}", e.getRequestId(), e.getReason());
            }
        } catch (Exception ex) {
            log.error("Failed to process TXN-COMPLETED message: {}", raw, ex);
        }
    }
}
