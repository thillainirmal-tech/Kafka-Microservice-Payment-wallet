package com.wallet.code.client;
import com.wallet.code.dto.WalletBalanceDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
@FeignClient(name = "wallet-service",url = "http://localhost:8084")
public interface WalletServiceClient {
    @GetMapping("/wallet-service/balance/{userId}")
    WalletBalanceDto getBalance(@PathVariable("userId") Long userId);
}