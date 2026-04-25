# Kafka Event Filter with Rule Engine

A Spring Boot application that consumes Kafka events from all topics (via regex pattern matching) and filters them based on simple table-based rules stored in MongoDB. Each rule defines up to 7 column-value conditions — if all match, the rule fires.

## Architecture

```
┌─────────────┐     ┌──────────────────────────────┐     ┌──────────────┐
│ Kafka Topics │────▶│  Kafka Event Filter Service   │────▶│ Target Topics│
│ (events.*)   │     │                              │     │ (filtered)   │
└─────────────┘     │  ┌────────────────────────┐  │     └──────────────┘
                    │  │   Table-Based Rules     │  │
                    │  │  column=value matching   │  │     ┌──────────────┐
                    │  │  (up to 7 conditions)   │  │◀───▶│   MongoDB    │
                    │  └────────────────────────┘  │     │  (Rules DB)  │
                    │  ┌────────────────────────┐  │     └──────────────┘
                    │  │  JSON → UPO → SWIFT    │  │
                    │  │  GPI Tracker API        │  │
                    │  └────────────────────────┘  │
                    └──────────────────────────────┘
```

## Features

- **Dynamic Topic Subscription**: Listens to all topics matching a configurable regex pattern (default: `events.*`)
- **Simple Table-Based Rules**: Each rule defines up to 7 column-value conditions — if all match, the rule fires
- **Rule Actions**: FORWARD (to filtered topic), DROP (discard), ROUTE (to specific topic)
- **Priority-based evaluation**: Rules are evaluated in priority order (lower = higher priority)
- **REST API**: Full CRUD for rules + rule testing endpoint + event publishing
- **Audit Trail**: All filtered events are persisted in MongoDB with metadata
- **Hot-reload**: Rule changes in MongoDB take effect immediately (no restart needed)
- **JSON to UPO Conversion**: Automatically converts Kafka JSON messages to Universal Payment Objects (SWIFT MT/MX fields)
- **SWIFT GPI Tracker Integration**: Calls `PUT /swift-apitracker/v5/payments/{uetr}/status` to update payment tracking status
- **Multi-alias field mapping**: Supports multiple field name aliases for flexible JSON-to-UPO mapping (e.g., `amount` or `instructedAmount`)

## Prerequisites

- Java 17+
- Docker & Docker Compose
- Maven 3.9+

## Quick Start

### 1. Start Infrastructure

```bash
docker-compose up -d
```

This starts:
- **MongoDB** on port `27017`
- **Kafka** (with Zookeeper) on port `9092`
- **Kafka UI** on port `8090` (http://localhost:8090)

### 2. Build & Run

```bash
./mvnw clean package -DskipTests
./mvnw spring-boot:run
```

The app starts on http://localhost:8080.

### 3. Create Filter Rules

Each rule defines up to 7 column-value conditions. An event matches when **all** non-null columns equal the event's field values.

#### Example: Route USD payments from a specific sender

```bash
curl -X POST http://localhost:8080/api/rules \
  -H "Content-Type: application/json" \
  -d '{
    "name": "usd-payments-from-deutsche",
    "description": "Route USD payments from Deutsche Bank",
    "topics": ["events.payments"],
    "column1": "currency",    "value1": "USD",
    "column2": "senderBic",   "value2": "DEUTDEFF",
    "column3": "status",      "value3": "ACSP",
    "action": "ROUTE",
    "targetTopic": "events.payments.usd-review",
    "priority": 10,
    "enabled": true
  }'
```

#### Example: Drop test events

```bash
curl -X POST http://localhost:8080/api/rules \
  -H "Content-Type: application/json" \
  -d '{
    "name": "drop-test-events",
    "description": "Drop events from test environment",
    "column1": "environment", "value1": "test",
    "action": "DROP",
    "priority": 1,
    "enabled": true
  }'
```

#### Example: Full 7-column rule

```bash
curl -X POST http://localhost:8080/api/rules \
  -H "Content-Type: application/json" \
  -d '{
    "name": "specific-payment-route",
    "description": "Route specific cross-border payments",
    "column1": "messageType",  "value1": "MT103",
    "column2": "currency",     "value2": "EUR",
    "column3": "senderBic",    "value3": "DEUTDEFF",
    "column4": "receiverBic",  "value4": "CHASUS33",
    "column5": "status",       "value5": "ACSP",
    "column6": "chargeBearer", "value6": "SHA",
    "column7": "environment",  "value7": "production",
    "action": "ROUTE",
    "targetTopic": "events.payments.cross-border",
    "priority": 5,
    "enabled": true
  }'
```

### 4. Test Rules

Test a rule against a sample event without publishing to Kafka:

```bash
curl -X POST http://localhost:8080/api/rules/test \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "events.payments",
    "eventPayload": {
      "severity": "CRITICAL",
      "source": "payment-service",
      "amount": 5000,
      "currency": "USD"
    }
  }'
```

### 5. Publish Test Events

```bash
curl -X POST http://localhost:8080/api/events/publish \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "events.payments",
    "key": "payment-123",
    "payload": {
      "severity": "CRITICAL",
      "source": "payment-service",
      "amount": 5000,
      "currency": "USD",
      "transactionId": "txn-abc-123"
    }
  }'
```

### 6. View Filtered Events

```bash
# Get recent filtered events
curl http://localhost:8080/api/events/filtered

# Get filtered events by topic
curl http://localhost:8080/api/events/filtered/topic/events.payments

# Get filtered events by rule
curl http://localhost:8080/api/events/filtered/rule/<rule-id>
```

## API Reference

### Rules API

| Method | Endpoint                      | Description                    |
|--------|-------------------------------|--------------------------------|
| POST   | `/api/rules`                  | Create a new filter rule       |
| GET    | `/api/rules`                  | List all rules                 |
| GET    | `/api/rules/{id}`             | Get a specific rule            |
| PUT    | `/api/rules/{id}`             | Update a rule                  |
| DELETE | `/api/rules/{id}`             | Delete a rule                  |
| GET    | `/api/rules/enabled`          | List enabled rules             |
| PATCH  | `/api/rules/{id}/toggle?enabled=true/false` | Enable/disable a rule |
| POST   | `/api/rules/test`             | Test rules against sample data |

### Events API

| Method | Endpoint                              | Description                    |
|--------|---------------------------------------|--------------------------------|
| POST   | `/api/events/publish`                 | Publish an event to Kafka      |
| GET    | `/api/events/filtered`                | Get recent filtered events     |
| GET    | `/api/events/filtered/topic/{topic}`  | Filtered events by topic       |
| GET    | `/api/events/filtered/rule/{ruleId}`  | Filtered events by rule        |

## Rule Matching

Each rule has up to 7 column-value pairs. When a Kafka event arrives, the engine checks all enabled rules in priority order. A rule matches if **every** non-null column in the rule equals the corresponding field in the event payload.

| Rule Field | Description |
|------------|-------------|
| `column1` / `value1` | First condition: event field name and expected value |
| `column2` / `value2` | Second condition (optional) |
| `column3` / `value3` | Third condition (optional) |
| `column4` / `value4` | Fourth condition (optional) |
| `column5` / `value5` | Fifth condition (optional) |
| `column6` / `value6` | Sixth condition (optional) |
| `column7` / `value7` | Seventh condition (optional) |

Supports dot notation for nested fields: `"debtor.address.country"`.

## SWIFT GPI Tracker Integration

### How It Works

1. **Kafka Listener** receives a JSON message from any `events.*` topic
2. **JsonToUpoConverter** maps the JSON fields to a `UniversalPaymentObject` (SWIFT MT/MX format)
3. If the UPO contains a **UETR** (Unique End-to-End Transaction Reference), the **SwiftGpiTrackerService** calls:
   ```
   PUT /swift-apitracker/v5/payments/{uetr}/status
   ```
4. The status update result is logged (and can be stored for audit)

### UPO Fields (Universal Payment Object)

| Field                      | JSON Aliases                                  | Description                         |
|---------------------------|-----------------------------------------------|-------------------------------------|
| `uetr`                    | `uetr`                                        | SWIFT GPI tracking ID (UUID)        |
| `transactionReference`    | `transactionReference`, `txnRef`, `field20`   | MT103 field 20                      |
| `instructedAmount`        | `instructedAmount`, `amount`                  | Payment amount                      |
| `instructedCurrency`      | `instructedCurrency`, `currency`, `ccy`       | ISO 4217 currency                   |
| `debtorAgentBic`          | `debtorAgentBic`, `senderBic`                 | Ordering institution BIC            |
| `creditorAgentBic`        | `creditorAgentBic`, `receiverBic`             | Beneficiary institution BIC         |
| `debtorName`              | `debtorName`, `originatorName`                | Originator name                     |
| `creditorName`            | `creditorName`, `beneficiaryName`             | Beneficiary name                    |
| `valueDate`               | `valueDate`, `valueDt`                        | Settlement date (YYYY-MM-DD)        |
| `transactionStatus`       | `transactionStatus`, `status`                 | GPI status (ACCC, ACSP, RJCT, etc.) |

### Example: Payment Event with GPI Tracking

```bash
curl -X POST http://localhost:8080/api/events/publish \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "events.payments",
    "key": "payment-456",
    "payload": {
      "uetr": "97ed4827-7b6f-4491-a06f-b548d5a7512d",
      "transactionReference": "REF20250425001",
      "messageType": "MT103",
      "amount": 50000,
      "currency": "USD",
      "senderBic": "DEUTDEFF",
      "receiverBic": "CHASUS33",
      "debtorName": "Acme Corp",
      "creditorName": "Global Trading Ltd",
      "beneficiaryAccount": "US64SVBK12345678901234",
      "valueDate": "2025-04-25",
      "status": "ACSP",
      "chargeBearer": "SHA",
      "remittanceInfo": "Invoice INV-2025-001"
    }
  }'
```

### SWIFT API Configuration

Set `SWIFT_API_ENABLED=true` and provide your credentials to enable live API calls:

```bash
export SWIFT_API_ENABLED=true
export SWIFT_API_BASE_URL=https://sandbox.swift.com   # or https://api.swift.com for production
export SWIFT_API_KEY=your-api-key
export SWIFT_CLIENT_ID=your-client-id
export SWIFT_CLIENT_SECRET=your-client-secret
export SWIFT_INSTITUTION_BIC=YOURBICXXXX
```

When `SWIFT_API_ENABLED=false` (default), the service runs in **simulation mode** — it logs the request and returns a simulated success response.

## Configuration

| Property                        | Default                | Description                      |
|---------------------------------|------------------------|----------------------------------|
| `spring.kafka.bootstrap-servers`| `localhost:9092`       | Kafka broker address             |
| `spring.data.mongodb.uri`       | `mongodb://localhost:27017/event_filter_db` | MongoDB connection |
| `kafka.topic-pattern`           | `events.*`             | Regex pattern for topic subscription |
| `spring.kafka.consumer.group-id`| `event-filter-group`   | Kafka consumer group ID          |
| `swift.api.base-url`            | `https://sandbox.swift.com` | SWIFT API base URL          |
| `swift.api.api-key`             | *(empty)*              | SWIFT API key / Bearer token     |
| `swift.api.institution-bic`     | *(empty)*              | Your institution's BIC           |
| `swift.api.enabled`             | `false`                | Enable live SWIFT API calls      |

## Monitoring

- **Health Check**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/metrics
- **Kafka UI**: http://localhost:8090
