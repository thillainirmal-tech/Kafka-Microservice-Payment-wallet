package com.wallet.code.dto;

import lombok.Data;

@Data
public class WalletBalanceDto {
    private Long userId;
    private Double balance;
    private String status;

    // getters + setters
}
