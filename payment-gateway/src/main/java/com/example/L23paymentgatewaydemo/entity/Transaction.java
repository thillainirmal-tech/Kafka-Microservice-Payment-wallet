package com.example.L23paymentgatewaydemo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Transaction {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String txnId;

    @Column(nullable = false)
    Long merchantId;

    // Transaction.java
    @Column(length = 36)
    private String gatewayOrderId;
    // rzp_order_...
    @Column(length = 36)
    private String gatewayPaymentId;

    // rzp_payment_...
    @Column(length = 128)
    private String gatewaySignature;

    // HMAC from Razorpay
    @Column(length = 8)
    private String currency;            // "INR"

    @Column(nullable = false)
    Long userId;

    @Column(nullable = false)
    Double amount;

    @Column(nullable = false)
    private String status;

    @CreationTimestamp
    private Date createdOn;

    @UpdateTimestamp
    private Date updatedOn;

}
