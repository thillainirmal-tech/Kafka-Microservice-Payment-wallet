package com.wallet.code.dto;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class WalletUpdatedPayload {

    private String userEmail;
    private Double balance;
    private String requestId;
}
