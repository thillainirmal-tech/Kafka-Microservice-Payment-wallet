package com.wallet.transaction.web;

import com.wallet.transaction.service.PaymentWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class WebhookController {

    private final PaymentWebhookService service;

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String rawBody,
                                        @RequestHeader("X-PG-Signature") String signature) {
        service.handle(rawBody, signature);
        return ResponseEntity.ok().build();
    }
}
