# Event Ledger

A distributed financial transaction processing system composed of two independently runnable microservices. The system receives financial transaction events, enforces idempotency, handles out-of-order event delivery, and maintains correct account balances at all times.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Project Structure](#project-structure)
- [API Reference](#api-reference)
- [Prerequisites](#prerequisites)
- [Setup and Installation](#setup-and-installation)
- [Running the Services](#running-the-services)
- [Running the Tests](#running-the-tests)
- [Distributed Tracing](#distributed-tracing)
- [Observability](#observability)
- [Resiliency Pattern](#resiliency-pattern)
- [Graceful Degradation](#graceful-degradation)
- [Design Decisions](#design-decisions)

---

## Architecture Overview

```
Browser / Client ──────→  Event Gateway Service  (port 8080)
                               │
                               │  REST (sync) + X-Trace-Id header
                               ▼
                          Account Service  (port 8081)
```

### Event Gateway Service (public-facing, port 8080)

The entry point for all client requests. Responsibilities:

- Accepts and validates incoming transaction events
- Enforces idempotency using `eventId` as the primary key — duplicate submissions return the original event without re-processing
- Persists events in its own embedded H2 database
- Calls the Account Service synchronously to apply each transaction
- Wraps all Account Service calls with a **circuit breaker** (Resilience4j)
- Generates a `traceId` for every request and propagates it downstream via the `X-Trace-Id` HTTP header
- Exposes event query endpoints that work independently from the Account Service

### Account Service (internal, port 8081)

Manages account state. Responsibilities:

- Applies transactions (CREDIT / DEBIT) to accounts
- Maintains idempotency at its own level — duplicate `eventId` submissions are silently ignored
- Auto-creates an account record on first transaction for a given `accountId`
- Computes balances as: `sum(CREDIT amounts) − sum(DEBIT amounts)`
- Returns transactions and balances sorted chronologically by `eventTimestamp`, ensuring correctness regardless of arrival order
- Exposes balance and account detail endpoints

### Service Interaction Contract

The Gateway calls the Account Service over HTTP with structured JSON payloads. Both services share no database or in-process state. All cross-service calls carry the `X-Trace-Id` header for end-to-end traceability.

---

## Project Structure

```
event-ledger/
├── event-gateway-service/          # Public-facing gateway (port 8080)
│   ├── src/main/java/com/eventledger/gateway/
│   │   ├── client/                 # Account Service HTTP client + DTOs
│   │   ├── common/                 # Tracing filter, exception handler, constants
│   │   ├── config/                 # WebClient configuration (trace propagation)
│   │   ├── interfaces/
│   │   │   ├── event/              # POST /events, GET /events endpoints + service
│   │   │   ├── account/            # GET /accounts/{id}/balance proxy endpoint
│   │   │   └── health/             # GET /health endpoint
│   │   └── observability/          # Micrometer metrics (counters)
│   └── src/test/java/              # Integration tests (WireMock)
│
├── account-service/                # Internal account management (port 8081)
│   ├── src/main/java/com/eventledger/account/
│   │   ├── common/                 # Tracing filter, exception handler, constants
│   │   ├── interfaces/
│   │   │   ├── transaction/        # POST /accounts/{id}/transactions
│   │   │   ├── ledger/             # GET /accounts/{id}/balance, GET /accounts/{id}
│   │   │   └── health/             # GET /health endpoint
│   │   └── observability/          # Micrometer metrics (counters)
│   └── src/test/java/              # Application context test
│
└── pom.xml                         # Parent POM (multi-module Maven project)
```

---

## API Reference

### Event Gateway Service — `http://localhost:8080`

#### `POST /events` — Submit a transaction event

Request body:

```json
{
  "eventId": "evt-001",
  "accountId": "acct-123",
  "type": "CREDIT",
  "amount": 150.00,
  "currency": "USD",
  "eventTimestamp": "2026-05-15T14:02:11Z",
  "metadata": {
    "source": "mainframe-batch",
    "batchId": "B-9042"
  }
}
```

| Field            | Type            | Required | Notes                          |
|------------------|-----------------|----------|--------------------------------|
| `eventId`        | string          | Yes      | Unique identifier for the event |
| `accountId`      | string          | Yes      | Target account                 |
| `type`           | string          | Yes      | `CREDIT` or `DEBIT`            |
| `amount`         | number          | Yes      | Must be greater than 0         |
| `currency`       | string          | Yes      | e.g. `USD`                     |
| `eventTimestamp` | ISO 8601 string | Yes      | When the event originally occurred |
| `metadata`       | object          | No       | Optional additional context    |

Responses:

| Status | Meaning                                      |
|--------|----------------------------------------------|
| `201`  | Event accepted and applied successfully      |
| `200`  | Duplicate `eventId` — returns original event |
| `400`  | Validation failure — missing/invalid fields  |
| `503`  | Account Service unavailable                  |

#### `GET /events/{eventId}` — Retrieve a single event

| Status | Meaning            |
|--------|--------------------|
| `200`  | Event found        |
| `404`  | Event not found    |

#### `GET /events?account={accountId}` — List events for an account

Returns all events for the account ordered by `eventTimestamp` ascending. Always reads from the Gateway's own database — works even when the Account Service is down.

| Status | Meaning                   |
|--------|---------------------------|
| `200`  | List of events (may be empty) |

#### `GET /accounts/{accountId}/balance` — Get current account balance

Proxies the request to the Account Service.

| Status | Meaning                                    |
|--------|--------------------------------------------|
| `200`  | `{ "accountId": "...", "balance": 0.00 }` |
| `503`  | Account Service unavailable               |

#### `GET /health` — Gateway health check

```json
{
  "service": "event-gateway-service",
  "status": "UP",
  "timestamp": "2026-05-15T14:02:11Z",
  "diagnostics": {
    "database": "UP",
    "description": "Event Gateway Service is running"
  }
}
```

---

### Account Service — `http://localhost:8081`

> Intended for internal use by the Gateway only, not exposed to external clients.

#### `POST /accounts/{accountId}/transactions` — Apply a transaction

#### `GET /accounts/{accountId}/balance` — Get current balance

Returns computed balance: `sum(CREDITs) − sum(DEBITs)`.

#### `GET /accounts/{accountId}` — Get account details and transaction history

Returns account ID and all transactions sorted by `eventTimestamp` ascending.

#### `GET /health` — Account Service health check

---

## Prerequisites

| Tool        | Version   |
|-------------|-----------|
| Java        | 17+       |
| Maven       | 3.8+      |

> No external database, message broker, or infrastructure setup is required. Both services use an embedded **H2 in-memory database** that is initialized automatically on startup.

---

## Setup and Installation

Clone the repository:

```bash
git clone <repository-url>
cd event-ledger
```

Build all modules from the root:

```bash
mvn clean install -DskipTests
```

Or build each service individually:

```bash
cd event-gateway-service
mvn clean install -DskipTests

cd ../account-service
mvn clean install -DskipTests
```

---

## Running the Services

Both services must be running simultaneously for the full flow to work. Start them in separate terminals.

**Terminal 1 — Account Service (start this first):**

```bash
cd account-service
mvn spring-boot:run
```

Account Service starts on **http://localhost:8081**

**Terminal 2 — Event Gateway Service:**

```bash
cd event-gateway-service
mvn spring-boot:run
```

Event Gateway starts on **http://localhost:8080**

### Verify both services are healthy

```bash
curl http://localhost:8080/health
curl http://localhost:8081/health
```

### Quick smoke test

Submit a transaction event:

```bash
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "evt-001",
    "accountId": "acct-123",
    "type": "CREDIT",
    "amount": 150.00,
    "currency": "USD",
    "eventTimestamp": "2026-05-15T14:02:11Z"
  }'
```

Check account balance:

```bash
curl http://localhost:8080/accounts/acct-123/balance
```

Submit a duplicate (idempotency check — returns `200` instead of `201`):

```bash
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "evt-001",
    "accountId": "acct-123",
    "type": "CREDIT",
    "amount": 150.00,
    "currency": "USD",
    "eventTimestamp": "2026-05-15T14:02:11Z"
  }'
```

List all events for an account:

```bash
curl "http://localhost:8080/events?account=acct-123"
```

### H2 Console (development)

Both services expose an H2 web console for inspecting the in-memory database:

| Service               | URL                              | JDBC URL                          |
|-----------------------|----------------------------------|-----------------------------------|
| Event Gateway Service | http://localhost:8080/h2-console | `jdbc:h2:mem:event_gateway_db`    |
| Account Service       | http://localhost:8081/h2-console | `jdbc:h2:mem:account_service_db`  |

Username: `sa` — Password: _(leave blank)_

---

## Running the Tests

Tests are located in the `event-gateway-service` module. They use **WireMock** to mock the Account Service, so no running services are needed.

Run all tests from the Gateway module:

```bash
cd event-gateway-service
mvn test
```

Or from the project root to run all modules:

```bash
mvn test
```

### What the tests cover

All tests live in `GatewayToAccountServiceIntegrationTest` and cover:

| Test | Requirement Covered |
|------|---------------------|
| `testFullGatewayToAccountServiceFlow_SuccessfulTransaction` | Full Gateway → Account Service flow, correct `201` response, trace header forwarded |
| `testFullGatewayToAccountServiceFlow_DuplicateEventIgnored` | Idempotency — second submission returns `200`, Account Service called only once |
| `testFullGatewayToAccountServiceFlow_AccountServiceUnavailable` | Resiliency — circuit breaker fallback returns `503 Service Unavailable` |
| `testFullGatewayToAccountServiceFlow_InvalidEventData` | Validation — missing `accountId` returns `400`, Account Service not called |
| `testFullGatewayToAccountServiceFlow_MultipleTransactionTypes` | DEBIT transaction type processed correctly |
| `testGetEventById_EventExists` | `GET /events/{id}` returns stored event |
| `testGetEventById_EventNotFound` | `GET /events/{id}` returns `404` for unknown event |
| `testGetEventsByAccount_ReturnsAllEventsForAccount` | `GET /events?account=` returns all events for an account |
| `testGetAccountBalance_FromAccountService` | Gateway proxies balance request, trace ID forwarded |
| `testGetAccountBalance_AccountServiceUnavailable` | Balance endpoint returns `503` when Account Service is down |
| `testGatewayHealthCheck` | `/health` returns service name, status, DB diagnostics, timestamp |
| `testTraceIdPropagationAcrossServices` | `X-Trace-Id` from client is propagated to Account Service unchanged |

---

## Distributed Tracing

Trace propagation is implemented without an external tracing library, using a simple and transparent approach:

1. The `TraceIdFilter` (servlet filter) runs on every request in both services
2. If the incoming request contains an `X-Trace-Id` header, that value is used
3. If not, a new UUID is generated
4. The trace ID is stored in **SLF4J MDC** under the key `traceId`
5. All structured log lines automatically include `traceId` via the logging pattern
6. The `WebClientConfig` bean injects the MDC trace ID into every outbound `WebClient` request header before it is sent to the Account Service
7. The `X-Trace-Id` header is echoed back in every HTTP response

This means a single client request produces a consistent `traceId` in log output across both services, making the full path traceable by searching logs for that one value.

**Log format (both services):**

```
2026-05-15T14:02:11.123Z level=INFO service=event-gateway-service traceId=abc-123 thread=http-nio-1 logger=EventServiceImpl message="Submitting event eventId=evt-001 accountId=acct-123"
```

---

## Observability

### Health Endpoints

Both services expose `GET /health` returning:
- Service name
- Status (`UP`)
- Timestamp
- Database connectivity status (via Spring Boot Actuator's `HealthEndpoint`)

Spring Boot Actuator endpoints are also available:
- `GET /actuator/health`
- `GET /actuator/metrics`
- `GET /actuator/circuitbreakers` (Gateway only)

### Custom Metrics

Both services expose custom **Micrometer counters**, visible via `GET /actuator/metrics/{metric-name}`:

**Event Gateway Service:**

| Metric name                     | Description                                       |
|---------------------------------|---------------------------------------------------|
| `event_accepted_count`          | Number of new events successfully accepted        |
| `event_duplicate_ignored_count` | Number of duplicate `eventId` submissions ignored |
| `account_service_failure_count` | Number of Account Service call failures           |

**Account Service:**

| Metric name                           | Description                               |
|---------------------------------------|-------------------------------------------|
| `transaction_success_count`           | Number of transactions applied            |
| `transaction_duplicate_ignored_count` | Number of duplicate transactions ignored  |

Example:

```bash
curl http://localhost:8080/actuator/metrics/event_accepted_count
```

---

## Resiliency Pattern

### Circuit Breaker (Resilience4j)

The Gateway wraps every call to the Account Service inside a **circuit breaker** using Spring Cloud Circuit Breaker backed by Resilience4j.

**Why circuit breaker over the other patterns:**

A circuit breaker is the most appropriate choice here because the Account Service is a stateful dependency. If it starts failing repeatedly, continuing to hammer it with requests makes recovery harder and ties up Gateway threads. The circuit breaker automatically stops forwarding requests after a failure threshold is crossed, gives the Account Service time to recover, and resumes traffic gradually via the half-open state — all without any manual intervention.

Retry with backoff alone would still cause the client to wait and would worsen load on a struggling downstream. A bulkhead would isolate thread exhaustion but wouldn't stop the flood of calls.

**Configuration (`application.properties` in Gateway):**

| Property | Value | Meaning |
|---|---|---|
| `failure-rate-threshold` | `50` | Opens the circuit when ≥50% of calls fail |
| `minimum-number-of-calls` | `3` | Minimum calls before the threshold is evaluated |
| `sliding-window-size` | `5` | Number of recent calls considered for failure rate |
| `wait-duration-in-open-state` | `10s` | How long the circuit stays open before trying half-open |
| `permitted-number-of-calls-in-half-open-state` | `2` | Calls allowed through in half-open to test recovery |
| `timeout-duration` | `2s` | Max wait time per call before counting as a failure |

**Behavior when the circuit is open:**

- `POST /events` → `503 Service Unavailable` with structured error body
- `GET /accounts/{id}/balance` → `503 Service Unavailable` with structured error body
- `GET /events/{id}` and `GET /events?account=` → **unaffected**, served from the Gateway's own database

---

## Graceful Degradation

| Endpoint | Account Service DOWN | Account Service UP |
|---|---|---|
| `POST /events` | `503` — circuit breaker fallback, event is NOT persisted to avoid inconsistency | Proceeds normally |
| `GET /events/{eventId}` | `200` — reads from Gateway DB, unaffected | `200` |
| `GET /events?account=` | `200` — reads from Gateway DB, unaffected | `200` |
| `GET /accounts/{id}/balance` | `503` — clear error, Account Service unreachable | `200` with balance |
| `GET /health` | `200` — Gateway health unaffected | `200` |

---

## Design Decisions

- **Java 17 + Spring Boot 3.5** — stable, widely supported, strong ecosystem for all required patterns
- **H2 in-memory database** — zero-infrastructure setup, each service has its own isolated instance; data is lost on restart which is appropriate for a demo system
- **`eventId` as primary key** — the simplest and most reliable idempotency mechanism; no separate idempotency table needed
- **Balance computed from transactions, not stored** — avoids the balance update race condition; the JPQL sum query is always consistent with the transaction log regardless of insertion order
- **`eventTimestamp` vs `createdAt`** — queries order by `eventTimestamp` (the business time), not `createdAt` (arrival time), so out-of-order delivery is handled naturally
- **WebClient (reactive) for outbound calls** — used for its non-blocking I/O and clean filter/interceptor model for trace injection, while the overall service remains servlet-based
- **MDC for trace propagation** — zero external dependency, thread-local safe in servlet context, automatically picked up by Logback pattern
