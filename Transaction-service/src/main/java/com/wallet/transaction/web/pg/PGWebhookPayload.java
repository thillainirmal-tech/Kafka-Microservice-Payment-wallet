package com.wallet.transaction.web.pg;

import lombok.Data;

@Data
public class PGWebhookPayload {
    private String txnId;        // must match wallet txnId/UUID
    private String status;       // SUCCESS | FAILED | PENDING
    private Double amount;
    private Long fromUserId;     // optional depending on your flow
    private Long toUserId;       // merchant/user to credit
    private String reason;       // failure reason (if any)
    private String pgRef;        // gateway reference
    private String occurredAt;   // ISO8601 (optional)
}
