package com.wallet.service.Repository;

import com.wallet.service.Model.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerRepo extends JpaRepository<LedgerEntry, Long> {
    boolean existsByTxnId(String txnId);
}
