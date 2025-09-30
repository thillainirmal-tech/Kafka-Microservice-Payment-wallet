package com.wallet.transaction.web.pg;

public interface SignatureVerifier {
    PGWebhookPayload parseAndVerify(String rawBody, String signature);
}
