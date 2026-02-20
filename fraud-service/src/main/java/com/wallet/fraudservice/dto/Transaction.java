package com.wallet.fraudservice.dto;

import lombok.Data;

@Data
public class Transaction {
    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private Double amount;
    private String requestId;
}
