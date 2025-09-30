package com.wallet.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.wallet.service","com.wallet.code"})
public class WalletApp {
    public static void main(String[] args) {
        SpringApplication.run(WalletApp.class, args);
    }
}
