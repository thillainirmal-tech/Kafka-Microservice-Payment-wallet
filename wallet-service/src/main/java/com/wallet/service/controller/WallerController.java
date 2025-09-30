package com.wallet.service.controller;

import com.wallet.code.dto.WalletBalanceDto;
import com.wallet.service.Service.WalletService;
import com.wallet.service.dto.AddMoneyRequest;
import com.wallet.service.dto.AddMoneyResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;


@RestController
@RequestMapping("/wallet-service")
public class WallerController {

    @Autowired
    private WalletService walletService;

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/balance/{userId}")
    public ResponseEntity<WalletBalanceDto> getWalletBalance(@PathVariable Long userId){
        WalletBalanceDto walletBalanceDto = walletService.walletBalance(userId);
        return ResponseEntity.ok(walletBalanceDto);
    }


    @PostMapping("/add-money")
    public ResponseEntity<AddMoneyResponse> addMoney(@RequestBody AddMoneyRequest addMoneyRequest){
        addMoneyRequest.setMerchantId(1l);
        AddMoneyResponse addMoneyResponse = restTemplate.postForObject("http://localhost:9090/pg-service/init-payment",addMoneyRequest,AddMoneyResponse.class);
        return ResponseEntity.ok(addMoneyResponse);
    }

    @GetMapping("/add-money-status/{pgTxnId}")
    public ResponseEntity<String> addMoneyStatus(@PathVariable String pgTxnId){
        return ResponseEntity.ok(walletService.processingPGTxn(pgTxnId));
    }



}
