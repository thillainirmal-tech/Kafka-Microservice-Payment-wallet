package com.wallet.transaction.web.pg;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class HmacSignatureVerifier implements SignatureVerifier {
    private static final Logger log = LoggerFactory.getLogger(HmacSignatureVerifier.class);
    private final ObjectMapper objectMapper;

    @Value("${pg.webhook.secret:demo-secret}") private String secret; // set in properties

    @Override
    public PGWebhookPayload parseAndVerify(String rawBody, String signature) {
        try {
            String expected = hmacSha256Hex(rawBody, secret);
            if (!constantTimeEquals(expected, signature)) {
                throw new IllegalArgumentException("Invalid PG signature");
            }
            return objectMapper.readValue(rawBody, PGWebhookPayload.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Webhook verification/parsing failed", e);
        }
    }

    private static String hmacSha256Hex(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Hex.encodeHexString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        int result = 0;
        for (int i = 0; i < Math.max(a.length(), b.length()); i++) {
            char ca = i < a.length() ? a.charAt(i) : 0;
            char cb = i < b.length() ? b.charAt(i) : 0;
            result |= ca ^ cb;
        }
        return result == 0;
    }
}
