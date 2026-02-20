# Kafka Microservice Payment Wallet

## Project Overview
Kafka Microservice Payment Wallet is a Java 21, Spring Boot 3.5.x multi-module system for wallet-based money movement. It uses event-driven communication through Apache Kafka to decouple services and adds AI-assisted fraud scoring in a dedicated `fraud-service`.

The repository is organized as a Maven reactor with these modules:
- `user-service`
- `wallet-service`
- `Transaction-service`
- `notification-service`
- `Common-CodeBase`
- `fraud-service`

## Architecture Description
The platform follows a distributed, event-driven microservice architecture:
- HTTP APIs are used for user and transaction initiation.
- Domain events are exchanged through Kafka topics.
- Each service owns its own responsibility and data lifecycle.
- Fraud analysis is performed asynchronously for transaction events and published for downstream consumers.

See [ARCHITECTURE.md](./ARCHITECTURE.md) for full architecture details.

## Microservices
### 1) user-service
Manages user lifecycle and profile operations.

### 2) wallet-service
Manages wallet data and balance-related operations.

### 3) Transaction-service
Accepts transaction requests, persists state, and publishes transaction-init events to Kafka topic `transactions`.

### 4) fraud-service
Consumes transaction events, calls Spring AI (Ollama) for classification, persists fraud results, and publishes outcomes to `fraud_results`.

### 5) notification-service
Consumes fraud results and sends alert emails for fraud-positive outcomes.

### 6) Common-CodeBase
Shared DTOs/config utilities reused across modules.

## Event Flow Explanation
1. A client creates a transaction via `Transaction-service`.
2. `Transaction-service` publishes transaction payloads to Kafka topic `transactions`.
3. `fraud-service` listens on `transactions`, sends structured prompts to Ollama, and computes a classification.
4. `fraud-service` stores the fraud result in DB and publishes to `fraud_results`.
5. `notification-service` consumes `fraud_results`; when `fraud=true`, it logs a warning and sends an email alert.

## Technologies Used
- Java 21
- Spring Boot 3.5.6
- Spring Kafka
- Spring AI (Ollama chat model)
- Spring Data JPA
- Maven multi-module build
- Apache Kafka + ZooKeeper
- Docker / Docker Compose
- Lombok
- JUnit + Mockito

## How to Run Locally
### Prerequisites
- Java 21
- Maven 3.9+
- Kafka broker (local or Docker)
- Ollama running at `http://localhost:11434` with model `llama3.1`
- MySQL (for services using persistence)

### Build all modules
```bash
mvn -DskipTests verify
```

### Run services individually (example)
```bash
mvn -pl Transaction-service spring-boot:run
mvn -pl fraud-service spring-boot:run
mvn -pl notification-service spring-boot:run
```

### Run tests for fraud-service
```bash
mvn -pl fraud-service test
```

## Docker Instructions
A root `docker-compose.yml` is included and currently provisions:
- `zookeeper`
- `kafka`
- `fraud-service`

### Start stack
```bash
docker compose up --build
```

### Stop stack
```bash
docker compose down
```

> `fraud-service` in Docker is configured with:
> - `SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092`
> - `FRAUD_KAFKA_PRODUCER_TOPIC=fraud_results`
> - `SPRING_AI_OLLAMA_BASE_URL=http://host.docker.internal:11434`

## AI Fraud Detection Explanation
`fraud-service` uses Spring AI `ChatClient` with Ollama:
- It sends a constrained prompt asking for strict JSON output.
- Expected response format:
  ```json
  {"classification":"FRAUD"|"SAFE","confidence":0.0}
  ```
- The response is parsed using Jackson.
- Invalid/empty responses and any model/parsing errors fallback to:
  ```json
  {"classification":"SAFE","confidence":0.0}
  ```

Detailed module-level explanation: [fraud-service/README.md](./fraud-service/README.md)

## Sample Transaction Payload
```json
{
  "id": 101,
  "fromUserId": 7,
  "toUserId": 19,
  "amount": 4500.75,
  "requestId": "a5f8a1ef-2bfa-4936-a5d7-4b1deac61f31"
}
```

## Future Improvements
- Add schema contracts and versioned event formats (Avro/JSON Schema).
- Implement outbox pattern for stronger DB/Kafka consistency.
- Add resilience patterns (retry/backoff, DLQ, circuit breakers).
- Add observability stack (OpenTelemetry tracing, metrics dashboards).
- Add API gateway and centralized authN/authZ.
- Add fraud model evaluation pipeline and explainability metrics.
- Add full containerization for all services in Compose.
