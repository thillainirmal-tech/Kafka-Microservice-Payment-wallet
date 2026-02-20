# Fraud Service API Documentation

## Endpoint: Analyze Transaction Fraud Risk

### `POST /fraud/analyze`
Analyzes an incoming transaction payload and returns AI-based fraud classification.

- **Content-Type:** `application/json`
- **Accept:** `application/json`

## Request JSON Example
```json
{
  "id": 101,
  "fromUserId": 7,
  "toUserId": 19,
  "amount": 4500.75,
  "requestId": "a5f8a1ef-2bfa-4936-a5d7-4b1deac61f31"
}
```

## Response JSON Example
### 200 OK (SAFE)
```json
{
  "classification": "SAFE",
  "confidence": 0.21
}
```

### 200 OK (FRAUD)
```json
{
  "classification": "FRAUD",
  "confidence": 0.93
}
```

## Error Handling Cases
The controller delegates inference to `FraudAIService`, which includes fallback handling:

1. **AI provider unavailable / runtime exception**
   - Behavior: service catches exception and returns fallback classification.
   - Response: `200 OK` with `{"classification":"SAFE","confidence":0.0}`.

2. **AI response not valid JSON**
   - Behavior: parsing exception is caught.
   - Response: `200 OK` with fallback SAFE payload.

3. **AI returns unsupported classification value**
   - Behavior: value normalized to `SAFE`.
   - Response: `200 OK` with normalized payload.

4. **Malformed request body**
   - Behavior: handled by Spring MVC/Jackson request binding.
   - Response: typically `400 Bad Request`.

5. **Unexpected server-side error outside fallback path**
   - Behavior: default Spring Boot exception flow.
   - Response: typically `500 Internal Server Error`.

## HTTP Status Codes
- `200 OK`: request processed and classification returned (including SAFE fallback cases).
- `400 Bad Request`: invalid JSON / deserialization issues in request body.
- `500 Internal Server Error`: unhandled server errors.

## Notes
- The endpoint currently has no explicit validation annotations on DTO fields.
- Fraud event publication happens in Kafka listener flow, not in this REST endpoint.
