package com.wallet.transaction.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.code.dto.TxnCompletedPayload;
import com.wallet.transaction.model.Transaction;
import com.wallet.transaction.model.TxnStatusEnum;
import com.wallet.transaction.repository.TransactionRepo;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.Optional;

@Configuration
public class TransactionConfigKafkaConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionConfigKafkaConsumer.class);


    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TransactionRepo transactionRepo;

    @KafkaListener(topics = "${txt.completed.topic}", groupId = "txn")
    public void consumeTransactionCompleted(ConsumerRecord<String, String> payload) throws JsonProcessingException {
        // guard nulls
        if (payload == null || payload.value() == null) {
            LOGGER.warn("Received null payload for completed topic; ignoring.");
            return;
        }

        TxnCompletedPayload txnCompletedPayload = objectMapper.readValue(payload.value(), TxnCompletedPayload.class);

        MDC.put("requestId", txnCompletedPayload.getRequestId());
        try {
            LOGGER.info("Received requestId={}", txnCompletedPayload.getRequestId());

            // fetch transaction safely
            Optional<Transaction> maybeTxn = transactionRepo.findById(txnCompletedPayload.getId());
            if (maybeTxn.isEmpty()) {
                LOGGER.error("Transaction not found for id={}", txnCompletedPayload.getId());
                return;
            }

            Transaction transaction = maybeTxn.get();

            // avoid Boolean unboxing NPE
            if (Boolean.TRUE.equals(txnCompletedPayload.getSuccess())) {
                transaction.setStatus(TxnStatusEnum.SUCCESS);
            } else {
                transaction.setStatus(TxnStatusEnum.FAILED);
                transaction.setReason(txnCompletedPayload.getReason());
            }

            transactionRepo.save(transaction);
        } finally {
            MDC.clear();
        }
    }
}
