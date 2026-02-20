# fraud-service

## Purpose of Fraud Detection
`fraud-service` evaluates transaction risk before downstream processing and notifies other services of fraud decisions.

Primary goals:
- classify transaction intent as `FRAUD` or `SAFE`
- provide confidence score in range `[0.0, 1.0]`
- persist fraud outcomes for auditability
- publish fraud outcomes for alerting workflows

## How Spring AI Works in This Project
The service uses Spring AI with Ollama via `ChatClient`:
1. `AiConfig` exposes a `ChatClient` bean.
2. `FraudAIService` builds structured prompts.
3. `chatClient.prompt().system(...).user(...).call().content()` retrieves model text.
4. Jackson parses the model output into structured fields.

Default model settings are configured under:
- `spring.ai.ollama.base-url`
- `spring.ai.ollama.chat.options.model`

## Prompt Structure
### System prompt (constraint)
The system prompt explicitly forces JSON-only responses in this schema:
```json
{"classification":"FRAUD"|"SAFE","confidence":<number>}
```

### User prompt (transaction context)
The user prompt provides structured values:
- `id`
- `fromUserId`
- `toUserId`
- `amount`
- `requestId`

This keeps inference grounded to transaction fields and improves parse reliability.

## JSON Contract Format
### AI expected JSON contract
```json
{
  "classification": "FRAUD" | "SAFE",
  "confidence": 0.0
}
```

### Kafka output (`FraudResult`)
```json
{
  "id": 101,
  "requestId": "a5f8a1ef-2bfa-4936-a5d7-4b1deac61f31",
  "fraud": false,
  "classification": "SAFE",
  "confidence": 0.18
}
```

### REST output (`FraudAiResponse`)
```json
{
  "classification": "SAFE",
  "confidence": 0.18
}
```

## Kafka Topics Used
- **Consumer topic:** `transactions`
  - Listener: `TransactionFraudListener#onTransaction`
- **Producer topic:** `fraud_results`
  - Publisher: `FraudResultProducer#publish`
  - Topic configurable via `fraud.kafka.producer.topic`

## How Embeddings Are Generated
Embeddings are **not currently generated** in this module.

Current implementation uses **chat-completion classification** (prompt -> JSON decision). If embedding-based retrieval or vector similarity is later introduced, it would require adding Spring AI embedding model dependencies, vector store integration, and a retrieval workflow.

## How Fallback Logic Works
`FraudAIService` applies defensive fallback handling:
- AI call throws exception -> fallback `SAFE`
- AI returns empty content -> fallback `SAFE`
- JSON parse fails -> fallback `SAFE`
- Unknown classification values -> normalized to `SAFE`
- Confidence outside range -> clamped to `[0.0, 1.0]`

Fallback response:
```json
{
  "classification": "SAFE",
  "confidence": 0.0
}
```

## Testing Instructions
### Run module unit tests
```bash
mvn -pl fraud-service test
```

### Run only FraudAIService tests
```bash
mvn -pl fraud-service -Dtest=FraudAIServiceTest test
```

### Build module without tests
```bash
mvn -pl fraud-service -DskipTests package
```

## API Documentation
See module API details in [`API_DOCUMENTATION.md`](./API_DOCUMENTATION.md).
