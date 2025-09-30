package com.wallet.service.Model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "wallet_ledger", uniqueConstraints = @UniqueConstraint(name="uk_ledger_txn", columnNames = "txnId"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LedgerEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=64)
    private String txnId;          // UNIQUE

    @Column(nullable=false)
    private Long userId;

    @Column(nullable=false)
    private Double amount;

    @Column(nullable=false, length=6)
    private String direction;      // "CREDIT" / "DEBIT"

    @Column(nullable=false)
    private Instant createdAt;

    public static LedgerEntry credit(String txnId, Long userId, Double amount) {
        return LedgerEntry.builder()
                .txnId(txnId).userId(userId).amount(amount)
                .direction("CREDIT").createdAt(Instant.now()).build();
    }
}
