package com.example.L23paymentgatewaydemo.service;

import com.example.L23paymentgatewaydemo.dto.PaymentInitResponse;
import com.example.L23paymentgatewaydemo.dto.PaymentPageRequest;
import com.example.L23paymentgatewaydemo.dto.TransactionDetailDto;
import com.example.L23paymentgatewaydemo.entity.Merchant;
import com.example.L23paymentgatewaydemo.entity.Transaction;
import com.example.L23paymentgatewaydemo.repo.MerchantRepo;
import com.example.L23paymentgatewaydemo.repo.TransactionRepo;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionService {

    @Autowired private TransactionRepo transactionRepo;
    @Autowired private MerchantRepo merchantRepo;

    @Value("${razorpay.key_id}")         private String keyId;
    @Value("${razorpay.key_secret}")     private String keySecret;
    @Value("${razorpay.webhook_secret}") private String webhookSecret;
    @Value("${razorpay.currency:INR}")   private String currency;

    public TransactionDetailDto getStatus(String txnId){
        Transaction t = getTransaction(txnId);
        return TransactionDetailDto.builder()
                .userId(t.getUserId())
                .status(t.getStatus())
                .amount(t.getAmount()) // DTO expects Double
                .build();
    }

    public Transaction getTransaction(String txnId){
        Transaction t = transactionRepo.findByTxnId(txnId);
        if (t == null) throw new IllegalArgumentException("Invalid txnId: " + txnId);
        return t;
    }

    /** Called by merchant app to start a payment page flow. */
    public PaymentInitResponse generatePaymentPage(PaymentPageRequest req){
        Assert.notNull(req.getMerchantId(), "merchantId required");
        Assert.notNull(req.getAmount(), "amount required");
        Assert.isTrue(BigDecimal.valueOf(req.getAmount()).compareTo(BigDecimal.ZERO) > 0, "amount must be > 0");

        merchantRepo.findById(req.getMerchantId())
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found: " + req.getMerchantId()));

        String txnId = UUID.randomUUID().toString();

        Transaction t = Transaction.builder()
                .merchantId(req.getMerchantId())
                .userId(req.getUserId())
                .txnId(txnId)
                .amount(req.getAmount()) // entity expects Double
                .currency(currency)
                .status("PENDING")
                .build();
        transactionRepo.save(t);

        String url = "http://localhost:9090/payment-page/" + txnId;
        return PaymentInitResponse.builder().txnId(txnId).url(url).build();
    }

    /** Create (or reuse) the Razorpay Order. */
    public Transaction ensureGatewayOrder(String txnId) throws Exception {
        Transaction t = getTransaction(txnId);
        if (t.getGatewayOrderId() != null) return t;

        long amountInPaise = BigDecimal
                .valueOf(t.getAmount())
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        JSONObject orderReq = new JSONObject();
        orderReq.put("amount", amountInPaise);
        orderReq.put("currency", t.getCurrency());            // "INR"
        orderReq.put("receipt", "txn_" + t.getTxnId());
        orderReq.put("payment_capture", 1);

        JSONObject notes = new JSONObject();
        notes.put("app_txn_id", t.getTxnId());
        notes.put("merchant_id", String.valueOf(t.getMerchantId()));
        orderReq.put("notes", notes);

        // ✅ RazorpayClient with lowercase .orders (SDK 1.4.x)
        RazorpayClient client = new RazorpayClient(keyId, keySecret);
        Order order = client.orders.create(orderReq);

        t.setGatewayOrderId(order.get("id")); // rzp_order_...
        transactionRepo.save(t);
        return t;
    }

    /** Verify signature after Checkout success. */
    public void verifyAndMarkPaid(String txnId, String razorpayOrderId, String razorpayPaymentId, String razorpaySignature){
        Transaction t = getTransaction(txnId);
        if (!razorpayOrderId.equals(t.getGatewayOrderId())) {
            throw new IllegalArgumentException("Order mismatch");
        }

        String signData = razorpayOrderId + "|" + razorpayPaymentId;
        String expected = hmacHexSHA256(signData, keySecret);

        if (!expected.equals(razorpaySignature)) {
            t.setStatus("FAILED");
            t.setGatewayPaymentId(razorpayPaymentId);
            t.setGatewaySignature(razorpaySignature);
            transactionRepo.save(t);
            throw new IllegalArgumentException("Invalid payment signature");
        }

        t.setStatus("SUCCESS");
        t.setGatewayPaymentId(razorpayPaymentId);
        t.setGatewaySignature(razorpaySignature);
        transactionRepo.save(t);
    }

    /** Webhook verification (raw body + header). */
    public boolean verifyWebhook(String rawBody, String headerSignature){
        String expected = hmacHexSHA256(rawBody, webhookSecret);
        return expected.equals(headerSignature);
    }

    private String hmacHexSHA256(String data, String secret){
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** After verify(), send user back to merchant. */
    public String doPaymentAndRedirect(String txnId){
        Transaction t = getTransaction(txnId);
        Optional<Merchant> m = merchantRepo.findById(t.getMerchantId());
        Merchant merchant = m.orElseThrow(() ->
                new IllegalArgumentException("Merchant not found: " + t.getMerchantId()));
        return merchant.getRedirectionUrl() + txnId;
    }
}
