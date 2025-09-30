package com.wallet.service.Service;

import com.wallet.code.dto.TxnCompletedPayload;
import com.wallet.code.dto.TxnInitPayload;
import com.wallet.code.dto.WalletBalanceDto;
import com.wallet.code.dto.WalletUpdatedPayload;
import com.wallet.service.Model.Wallet;
import com.wallet.service.Repository.WalletRepo;
import com.wallet.service.dto.PGPaymentStatusDTO;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Service
public class WalletService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WalletService.class);

    @Autowired
    private KafkaTemplate<String,Object> kafkaTemplate;

    @Autowired
    private WalletRepo walletRepo;

    @Value("${wallet.completed.topic}")
    private String TXTCOMPLETED;

    @Value("${wallet.updated.topic}")
    private String WALLETUPDATED;

    @Autowired
    private com.wallet.service.Repository.LedgerRepo ledgerRepo;

    @Autowired
    private RestTemplate restTemplate;

    public WalletBalanceDto walletBalance(Long userId) {
        Wallet wallet = walletRepo.findByUserId(userId);
        if (wallet == null) {
            // Make it a 404 so Feign can handle it gracefully
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Wallet not found for userId=" + userId);
        }
        WalletBalanceDto dto = new WalletBalanceDto();
        dto.setUserId(userId);                   // include userId (helpful for clients)
        dto.setBalance(wallet.getBalance());     // consider BigDecimal for money
        dto.setStatus("OK");
        return dto;
    }

    @Transactional
    public void walletTxn(TxnInitPayload txnInitPayload) throws ExecutionException, InterruptedException {
        TxnCompletedPayload txnCompletedPayload = new TxnCompletedPayload();
        txnCompletedPayload.setId(txnInitPayload.getId());
        txnCompletedPayload.setRequestId(txnInitPayload.getRequestId());

        Wallet fromwallet = walletRepo.findByUserId(txnInitPayload.getFromUserId());
        Wallet towallet   = walletRepo.findByUserId(txnInitPayload.getToUserId());

        // Validate wallets exist
        if (fromwallet == null || towallet == null) {
            txnCompletedPayload.setSuccess(false);
            txnCompletedPayload.setReason("Wallet not found");
            Future<SendResult<String,Object>> f = kafkaTemplate.send(
                    TXTCOMPLETED, txnInitPayload.getRequestId(), txnCompletedPayload);
            LOGGER.info("Pushed into kafka TXTCOMPLETED (wallet missing): " + f.get());
            return;
        }

        // Check balance
        if (fromwallet.getBalance() < txnInitPayload.getAmount()) {
            txnCompletedPayload.setSuccess(false);
            txnCompletedPayload.setReason("Insufficient balance");
            Future<SendResult<String,Object>> f = kafkaTemplate.send(
                    TXTCOMPLETED, txnInitPayload.getRequestId(), txnCompletedPayload);
            LOGGER.info("Pushed into kafka TXTCOMPLETED (insufficient): " + f.get());
            return;
        }

        // Perform transfer
        fromwallet.setBalance(fromwallet.getBalance() - txnInitPayload.getAmount());
        towallet.setBalance(towallet.getBalance() + txnInitPayload.getAmount());

        // Persist updates explicitly to avoid repo implementation edge cases
        walletRepo.save(fromwallet);
        walletRepo.save(towallet);

        txnCompletedPayload.setSuccess(true);

        // Emit per-wallet update events using the current requestId as correlation/key
        WalletUpdatedPayload fromwalletUpdatedPayload = new WalletUpdatedPayload(
                fromwallet.getUserEmail(), fromwallet.getBalance(), txnInitPayload.getRequestId()
        );
        WalletUpdatedPayload towalletUpdatedPayload = new WalletUpdatedPayload(
                towallet.getUserEmail(), towallet.getBalance(), txnInitPayload.getRequestId()
        );

        Future<SendResult<String,Object>> future1 = kafkaTemplate.send(
                WALLETUPDATED, fromwalletUpdatedPayload.getUserEmail(), fromwalletUpdatedPayload);
        LOGGER.info("Pushed into kafka WALLETUPDATED fromWallet: " + future1.get());

        Future<SendResult<String,Object>> future2 = kafkaTemplate.send(
                WALLETUPDATED, towalletUpdatedPayload.getUserEmail(), towalletUpdatedPayload);
        LOGGER.info("Pushed into kafka WALLETUPDATED toWallet: " + future2.get());

        // Finally emit the transaction completion event (send the *TxnCompletedPayload*, not Wallet)
        Future<SendResult<String,Object>> finalFuture = kafkaTemplate.send(
                TXTCOMPLETED, txnInitPayload.getRequestId(), txnCompletedPayload);
        LOGGER.info("Pushed into kafka TXTCOMPLETED (success): " + finalFuture.get());
    }

    public String processingPGTxn(String pgtxn){
        PGPaymentStatusDTO paymentStatusDTO =
                restTemplate.getForObject("http://localhost:9090/pg-service/payment-status/" + pgtxn,
                        PGPaymentStatusDTO.class);

        if (paymentStatusDTO == null || paymentStatusDTO.getStatus() == null) {
            return "user updated failed";
        }

        if (paymentStatusDTO.getStatus().equalsIgnoreCase("SUCCESS")) {
            Wallet wallet = walletRepo.findByUserId(paymentStatusDTO.getUserId());
            if (wallet == null) {
                return "user updated failed";
            }
            wallet.setBalance(wallet.getBalance() + paymentStatusDTO.getAmount());
            walletRepo.save(wallet);
            return "user updated successfully";
        }
        return "user updated failed";
    }

    @Transactional
    public void applyPgCapture(com.wallet.code.dto.TxnCompletedPayload e) {
        String txnId = e.getRequestId();
        if (ledgerRepo.existsByTxnId(txnId)) {
            LOGGER.info("txnId={} already applied; skipping", txnId);
            return;
        }

        // Business rule: credit the receiver (toUserId). Adjust to your use-case.
        Long creditUser = e.getToUserId();
        Wallet w = walletRepo.findByUserId(creditUser);
        if (w == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found for userId=" + creditUser);
        }

        w.setBalance(w.getBalance() + e.getAmount());
        walletRepo.save(w);

        ledgerRepo.save(com.wallet.service.Model.LedgerEntry.credit(txnId, creditUser, e.getAmount()));

        // optional: publish wallet.updated (you already do for other flows)
        WalletUpdatedPayload payload = new WalletUpdatedPayload(w.getUserEmail(), w.getBalance(), txnId);
        kafkaTemplate.send(WALLETUPDATED, w.getUserEmail(), payload);
    }


}
