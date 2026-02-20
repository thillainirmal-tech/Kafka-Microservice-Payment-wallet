# ARCHITECTURE

## 1) Event-Driven Architecture Using Kafka
This project uses asynchronous messaging to decouple service responsibilities and reduce direct runtime coupling.

Core event channels currently used:
- `transactions`: transaction-init payloads emitted by `Transaction-service`.
- `fraud_results`: fraud outcomes emitted by `fraud-service`.

Kafka allows each service to evolve independently while still participating in end-to-end transaction workflows.

## 2) Transaction Flow Diagram Description
Conceptual sequence:
1. Client sends a transaction request to `Transaction-service`.
2. `Transaction-service` validates/persists transaction data and publishes a transaction-init event to `transactions`.
3. `fraud-service` consumes from `transactions`.
4. `fraud-service` asks Ollama (through Spring AI) to classify the transaction.
5. `fraud-service` stores the fraud decision in DB (`fraud_results` table via JPA entity).
6. `fraud-service` publishes a `FraudResult` event to `fraud_results`.
7. `notification-service` consumes `fraud_results`; if flagged as fraud, it logs and sends alert email.

## 3) How fraud-service Integrates
`fraud-service` has both synchronous and asynchronous integration paths:
- **Asynchronous Kafka path**
  - Consumer: topic `transactions`
  - Producer: topic `fraud_results`
- **Synchronous REST path**
  - `POST /fraud/analyze` accepts a transaction payload and returns AI classification.

It acts as an isolated fraud decision engine that can serve real-time API requests and event-based pipeline processing.

## 4) How Spring AI Processes Transactions
In `FraudAIService`:
- A strict system prompt constrains output to JSON with `classification` and `confidence`.
- A transaction-specific user prompt injects structured fields (`id`, `fromUserId`, `toUserId`, `amount`, `requestId`).
- The raw model output is parsed using Jackson.
- Safety guards:
  - Empty response -> fallback
  - Parse error -> fallback
  - Invalid classification value -> normalized to `SAFE`
  - Confidence is clamped to `[0.0, 1.0]`

Fallback output is always:
- `classification=SAFE`
- `confidence=0.0`

## 5) DTO Schema Explanation
### Transaction DTO (input)
```json
{
  "id": 101,
  "fromUserId": 7,
  "toUserId": 19,
  "amount": 4500.75,
  "requestId": "uuid-or-business-id"
}
```

### FraudClassificationResponse DTO (internal AI result)
```json
{
  "classification": "FRAUD | SAFE",
  "confidence": 0.0
}
```

### FraudResult DTO (Kafka output)
```json
{
  "id": 101,
  "requestId": "uuid-or-business-id",
  "fraud": true,
  "classification": "FRAUD",
  "confidence": 0.92
}
```

### FraudAiResponse DTO (REST output)
```json
{
  "classification": "SAFE",
  "confidence": 0.12
}
```

## 6) Service-to-Service Communication
- **HTTP**
  - Clients -> `Transaction-service` APIs
  - Clients/tools -> `fraud-service` `POST /fraud/analyze`
- **Kafka**
  - `Transaction-service` -> (`transactions`) -> `fraud-service`
  - `fraud-service` -> (`fraud_results`) -> `notification-service`

This mixed style (REST + eventing) enables both request/response interactions and scalable event processing.

## 7) Deployment Architecture Using Docker
Current `docker-compose.yml` provisions infrastructure and fraud processing runtime:
- `zookeeper`
- `kafka`
- `fraud-service`

`fraud-service` container configuration:
- Exposes port `8086`
- Uses Kafka at `kafka:9092`
- Reads Ollama at `http://host.docker.internal:11434`

For full platform deployment, additional services can be added to Compose with consistent Kafka/bootstrap and DB environment variables.
