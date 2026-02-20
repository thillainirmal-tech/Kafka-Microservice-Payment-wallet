package com.wallet.fraudservice.repository;

import com.wallet.fraudservice.model.FraudResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FraudResultRepository extends JpaRepository<FraudResultEntity, Long> {
}
