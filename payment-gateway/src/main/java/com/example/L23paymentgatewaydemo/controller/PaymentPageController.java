package com.example.L23paymentgatewaydemo.controller;

import com.example.L23paymentgatewaydemo.entity.Merchant;
import com.example.L23paymentgatewaydemo.entity.Transaction;
import com.example.L23paymentgatewaydemo.repo.MerchantRepo;
import com.example.L23paymentgatewaydemo.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/payment-page")
public class PaymentPageController {

    @Autowired private TransactionService transactionService;
    @Autowired private MerchantRepo merchantRepo;

    @Value("${razorpay.key_id}") private String keyId;

    @GetMapping("/{txnId}")
    public ModelAndView page(@PathVariable String txnId) throws Exception {
        // Ensure we have a Razorpay Order
        Transaction tx = transactionService.ensureGatewayOrder(txnId);
        Merchant merchant = merchantRepo.findById(tx.getMerchantId())
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));

        ModelAndView mv = new ModelAndView("paymentpage.html");
        mv.getModelMap().put("merchantName", merchant.getName());
        mv.getModelMap().put("amount", tx.getAmount());
        mv.getModelMap().put("txnId", txnId);

        // Razorpay Checkout params
        mv.getModelMap().put("rzpKey", keyId);
        mv.getModelMap().put("orderId", tx.getGatewayOrderId());
        mv.getModelMap().put("currency", tx.getCurrency());
        mv.getModelMap().put("verifyUrl", "/pg-service/verify/" + txnId);

        return mv;
    }
}
