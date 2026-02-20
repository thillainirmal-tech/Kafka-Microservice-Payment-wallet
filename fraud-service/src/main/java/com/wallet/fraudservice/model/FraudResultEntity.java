package com.wallet.fraudservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "fraud_results")
@Getter
@Setter
public class FraudResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "request_id", nullable = false)
    private String requestId;

    @Column(name = "is_fraud", nullable = false)
    private boolean fraud;

    @Column(name = "classification", nullable = false)
    private String classification;

    @Column(name = "confidence", nullable = false)
    private double confidence;
}
