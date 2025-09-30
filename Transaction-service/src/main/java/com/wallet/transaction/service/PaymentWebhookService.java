package com.wallet.transaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.code.dto.TxnCompletedPayload;
import com.wallet.transaction.model.Transaction;
import com.wallet.transaction.model.TxnStatusEnum;
import com.wallet.transaction.repository.TransactionRepo;
import com.wallet.transaction.web.pg.PGWebhookPayload;
import com.wallet.transaction.web.pg.SignatureVerifier;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentWebhookService {
    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookService.class);

    private final SignatureVerifier verifier;
    private final ObjectMapper objectMapper;
    private final TransactionRepo transactionRepo;
    private final KafkaTemplate<String, Object> kafka;

    @Value("${txt.completed.topic}") private String TXN_COMPLETED;
    @Value("${txt.failed.topic}")    private String TXN_FAILED;

    @Transactional
    public void handle(String rawBody, String signature) {
        // 1) Verify + parse PG payload
        PGWebhookPayload p = verifier.parseAndVerify(rawBody, signature);

        // 2) Upsert by txnId (shared correlation id)
        Transaction tx = transactionRepo.findByTxnId(p.getTxnId());
        if (tx == null) {
            tx = new Transaction();
            tx.setTxnId(p.getTxnId());
            tx.setFromUserId(p.getFromUserId());
            tx.setToUserId(p.getToUserId());
            tx.setAmount(p.getAmount());
        }

        if (tx.getStatus() != null && tx.getStatus().isTerminal()) {
            log.info("txnId={} already terminal: {}", tx.getTxnId(), tx.getStatus());
            return;
        }

        switch (p.getStatus().toUpperCase()) {
            case "SUCCESS", "PAID", "CAPTURED" -> tx.setStatus(TxnStatusEnum.SUCCESS);
            case "FAILED" -> { tx.setStatus(TxnStatusEnum.FAILED); tx.setReason(p.getReason()); }
            default -> tx.setStatus(TxnStatusEnum.PENDING);
        }
        transactionRepo.save(tx);

        // 3) Publish completion/failure to Kafka for wallet-service
        var evt = new TxnCompletedPayload();
        evt.setId(tx.getId());
        evt.setRequestId(tx.getTxnId());   // 👈 SAME across DBs/services
        evt.setFromUserId(tx.getFromUserId());
        evt.setToUserId(tx.getToUserId());
        evt.setAmount(tx.getAmount());
        evt.setSuccess(tx.getStatus() == TxnStatusEnum.SUCCESS);
        if (!evt.getSuccess()) evt.setReason(tx.getReason());

        String topic = evt.getSuccess() ? TXN_COMPLETED : TXN_FAILED;
        kafka.send(topic, evt.getRequestId(), evt);
    }
}
