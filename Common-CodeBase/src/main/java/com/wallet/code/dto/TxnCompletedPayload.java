package com.wallet.code.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TxnCompletedPayload {
    private Long id;          // Transaction row id (optional for consumers)
    private Boolean success;  // true/false from PG or wallet processing
    private String reason;    // failure reason if success=false
    private String requestId; // correlation id = txnId (UUID)

    // 👇 Add these so wallet-service can update balances
    private Double amount;    // amount involved in the txn
    private Long fromUserId;  // payer
    private Long toUserId;    // payee
}
