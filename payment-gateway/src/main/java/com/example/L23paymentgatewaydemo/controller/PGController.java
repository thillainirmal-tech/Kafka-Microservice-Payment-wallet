package com.example.L23paymentgatewaydemo.controller;

import com.example.L23paymentgatewaydemo.dto.PaymentInitResponse;
import com.example.L23paymentgatewaydemo.dto.PaymentPageRequest;
import com.example.L23paymentgatewaydemo.dto.TransactionDetailDto;
import com.example.L23paymentgatewaydemo.entity.Transaction;
import com.example.L23paymentgatewaydemo.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/pg-service")
public class PGController {

    @Autowired private TransactionService transactionService;

    @GetMapping("/payment-status/{txnId}")
    public ResponseEntity<TransactionDetailDto> getStatus(@PathVariable String txnId){
        return ResponseEntity.ok(transactionService.getStatus(txnId));
    }

    /** Keeps your old endpoint (not used by Razorpay flow). */
    @PostMapping("/doPayment/{txnId}")
    public ResponseEntity<String> doPayment(@PathVariable String txnId) {
        String url = transactionService.doPaymentAndRedirect(txnId);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(url));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @PostMapping("/init-payment")
    public ResponseEntity<PaymentInitResponse> initPayment(@RequestBody PaymentPageRequest pageRequest){
        return ResponseEntity.ok(transactionService.generatePaymentPage(pageRequest));
    }

    /** Called by your page's JS handler after Checkout "success". */
    @PostMapping("/verify/{txnId}")
    public ResponseEntity<?> verify(
            @PathVariable String txnId,
            @RequestParam("razorpay_order_id") String orderId,
            @RequestParam("razorpay_payment_id") String paymentId,
            @RequestParam("razorpay_signature") String signature
    ){
        transactionService.verifyAndMarkPaid(txnId, orderId, paymentId, signature);
        // After verifying server-side, redirect user to merchant
        String url = transactionService.doPaymentAndRedirect(txnId);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(url));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    /** Webhook: background confirmation (idempotent, robust). */
    @PostMapping(value = "/webhook/razorpay", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> webhook(HttpServletRequest request,
                                          @RequestHeader("X-Razorpay-Signature") String signature) throws Exception {
        String raw = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);

        if (!transactionService.verifyWebhook(raw, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid webhook signature");
        }

        // Minimal processing: update by order_id/payment_id (optional refinement)
        // You can parse JSON and switch on event type "payment.captured", etc.
        // Keep this idempotent.

        return ResponseEntity.ok("OK");
    }
}
