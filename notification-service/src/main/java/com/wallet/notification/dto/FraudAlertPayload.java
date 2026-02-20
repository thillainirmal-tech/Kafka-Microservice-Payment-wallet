package com.wallet.notification.dto;

import lombok.Data;

@Data
public class FraudAlertPayload {
    private Long id;
    private String requestId;
    private boolean fraud;
    private String classification;
    private double confidence;
}
