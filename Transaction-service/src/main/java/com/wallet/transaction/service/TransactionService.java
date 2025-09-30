package com.wallet.transaction.service;

import com.wallet.code.dto.TxnInitPayload;
import com.wallet.transaction.dto.TxnRequestDto;
import com.wallet.transaction.dto.TxnStatusDto;
import com.wallet.transaction.model.Transaction;
import com.wallet.transaction.model.TxnStatusEnum;
import com.wallet.transaction.repository.TransactionRepo;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


@Service
public class TransactionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionService.class);

    @Value("${txt.init.topic}")
    private String txninittopic;

    @Autowired
    private TransactionRepo transactionRepo;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public String initTransaction(TxnRequestDto txnRequestDto) throws ExecutionException, InterruptedException {
        // Basic null checks to avoid NPEs during persistence / send
        if (txnRequestDto == null ||
                txnRequestDto.getFromUserId() == null ||
                txnRequestDto.getToUserId() == null ||
                txnRequestDto.getAmount() == null) {
            throw new IllegalArgumentException("Invalid transaction request");
        }

        Transaction transaction = new Transaction();
        transaction.setFromUserId(txnRequestDto.getFromUserId());
        transaction.setToUserId(txnRequestDto.getToUserId());
        transaction.setAmount(txnRequestDto.getAmount());
        transaction.setComment(txnRequestDto.getComment());
        transaction.setTxnId(UUID.randomUUID().toString());
        transaction.setStatus(TxnStatusEnum.PENDING);
        transaction = transactionRepo.save(transaction);

        TxnInitPayload txnInitPayload = new TxnInitPayload();
        txnInitPayload.setId(transaction.getId());
        txnInitPayload.setFromUserId(transaction.getFromUserId());
        txnInitPayload.setToUserId(transaction.getToUserId());
        txnInitPayload.setAmount(transaction.getAmount());
        txnInitPayload.setRequestId(transaction.getTxnId());

        // NOTE: This send happens inside @Transactional and can cause DB/Kafka inconsistency if commit fails.
        Future<SendResult<String, Object>> future =
                kafkaTemplate.send(txninittopic, transaction.getFromUserId().toString(), txnInitPayload);
        LOGGER.info("Sending transaction to Kafka: {}", future.get());

        return transaction.getTxnId();
    }

    public TxnStatusDto getStatus(String transactionId) {
        Transaction transaction = transactionRepo.findByTxnId(transactionId);
        if (transaction == null) {
            // Signal not found to avoid ambiguous all-null fields
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }
        TxnStatusDto dto = new TxnStatusDto();
        dto.setReason(transaction.getReason());
        dto.setStatus(transaction.getStatus().toString());
        return dto;
    }
}

