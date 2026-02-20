package com.wallet.fraudservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FraudClassificationResponse {
    private String classification;
    private double confidence;
}
