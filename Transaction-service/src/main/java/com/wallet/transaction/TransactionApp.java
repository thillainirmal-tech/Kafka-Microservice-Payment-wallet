package com.wallet.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.wallet.transaction","com.wallet.code"})
public class TransactionApp {
    public static void main(String[] args) {
        SpringApplication.run(TransactionApp.class, args);
    }
}
