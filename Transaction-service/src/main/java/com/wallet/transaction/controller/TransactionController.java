package com.wallet.transaction.controller;

import com.wallet.transaction.dto.TxnRequestDto;
import com.wallet.transaction.dto.TxnStatusDto;
import com.wallet.transaction.service.TransactionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/transaction-service")
public class TransactionController {
    private static Logger LOGGER = LoggerFactory.getLogger(TransactionController.class);

    @Autowired
    private TransactionService transactionService;

    @PostMapping("/transaction")
    public ResponseEntity<String> initTransaction(@RequestBody @Valid TxnRequestDto txnRequestDto) throws ExecutionException, InterruptedException {
        LOGGER.info("Starting Transaction :{}", txnRequestDto);
        String txnid=transactionService.initTransaction(txnRequestDto);
        return ResponseEntity.accepted().body(txnid);
    }

    @GetMapping("/status/{txnId}")
    public ResponseEntity<TxnStatusDto> getTxnStatus(@PathVariable String txnId) {
        return ResponseEntity.ok(transactionService.getStatus(txnId));
    }
}
