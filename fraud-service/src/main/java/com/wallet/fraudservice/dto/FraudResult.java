package com.wallet.fraudservice.dto;

import lombok.Data;

@Data
public class FraudResult {
    private Long id;
    private String requestId;
    private boolean fraud;
    private String classification;
    private double confidence;
}
