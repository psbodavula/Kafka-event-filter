# Kafka Event Filter with Rule Engine

A Spring Boot application that consumes Kafka events from all topics (via regex pattern matching) and filters them based on rules stored in MongoDB. Supports a complex rule engine with **SpEL expressions**, **field-based matching**, and **composite rules**.

## Architecture

```
┌─────────────┐     ┌──────────────────────────────┐     ┌──────────────┐
│ Kafka Topics │────▶│  Kafka Event Filter Service   │────▶│ Target Topics│
│ (events.*)   │     │                              │     │ (filtered)   │
└─────────────┘     │  ┌────────────────────────┐  │     └──────────────┘
                    │  │    Rule Engine          │  │
                    │  │  ┌───────┐ ┌──────────┐│  │     ┌──────────────┐
                    │  │  │ SpEL  │ │Field Match││  │     │   MongoDB    │
                    │  │  └───────┘ └──────────┘│  │◀───▶│  (Rules DB)  │
                    │  │  ┌──────────────────┐  │  │     └──────────────┘
                    │  │  │   Composite      │  │  │
                    │  │  └──────────────────┘  │  │
                    │  └────────────────────────┘  │
                    └──────────────────────────────┘
```

## Features

- **Dynamic Topic Subscription**: Listens to all topics matching a configurable regex pattern (default: `events.*`)
- **Rule Types**:
  - **SpEL**: Full Spring Expression Language support for complex conditions
  - **FIELD_MATCH**: Field-level filtering with operators (EQUALS, CONTAINS, REGEX, GREATER_THAN, IN, etc.)
  - **COMPOSITE**: Combine multiple rules with AND/OR logic
- **Rule Actions**: FORWARD (to filtered topic), DROP (discard), ROUTE (to specific topic)
- **Priority-based evaluation**: Rules are evaluated in priority order (lower = higher priority)
- **REST API**: Full CRUD for rules + rule testing endpoint + event publishing
- **Audit Trail**: All filtered events are persisted in MongoDB with metadata
- **Hot-reload**: Rule changes in MongoDB take effect immediately (no restart needed)

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

#### SpEL Rule Example
Filter critical events from payment service:

```bash
curl -X POST http://localhost:8080/api/rules \
  -H "Content-Type: application/json" \
  -d '{
    "name": "critical-payment-events",
    "description": "Forward critical payment service events",
    "topics": ["events.payments"],
    "ruleType": "SPEL",
    "spelExpression": "#event['\''severity'\''] == '\''CRITICAL'\'' && #event['\''source'\''] == '\''payment-service'\''",
    "action": "FORWARD",
    "targetTopic": "events.alerts",
    "priority": 10,
    "enabled": true
  }'
```

#### Field Match Rule Example
Filter events by multiple field conditions:

```bash
curl -X POST http://localhost:8080/api/rules \
  -H "Content-Type: application/json" \
  -d '{
    "name": "high-value-orders",
    "description": "Route high-value orders for review",
    "topics": ["events.orders"],
    "ruleType": "FIELD_MATCH",
    "fieldConditions": {
      "amount": {"operator": "GREATER_THAN", "value": 1000},
      "status": {"operator": "EQUALS", "value": "PENDING"},
      "country": {"operator": "IN", "value": ["US", "UK", "DE"]}
    },
    "action": "ROUTE",
    "targetTopic": "events.orders.high-value",
    "priority": 20,
    "enabled": true
  }'
```

#### Composite Rule Example
Combine multiple rules with AND/OR:

```bash
# First create child rules, then create composite rule referencing their IDs
curl -X POST http://localhost:8080/api/rules \
  -H "Content-Type: application/json" \
  -d '{
    "name": "fraud-detection-composite",
    "description": "Composite fraud detection rule",
    "ruleType": "COMPOSITE",
    "childRuleIds": ["<rule-id-1>", "<rule-id-2>"],
    "compositeOperator": "AND",
    "action": "ROUTE",
    "targetTopic": "events.fraud-alerts",
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

## Rule Types

### SpEL (Spring Expression Language)

Access event fields using `#event['fieldName']` or `#fieldName`:

```
#event['severity'] == 'CRITICAL'
#amount > 1000 && #currency == 'USD'
#event['tags'].contains('urgent')
#event['nested']['field'] != null
```

### FIELD_MATCH

Operators: `EQUALS`, `NOT_EQUALS`, `CONTAINS`, `REGEX`, `GREATER_THAN`, `LESS_THAN`, `IN`, `NOT_IN`, `EXISTS`

Supports dot notation for nested fields: `"user.address.country"`.

### COMPOSITE

Combine rules with `AND` or `OR` logic. Reference child rules by their MongoDB IDs.

## Configuration

| Property                        | Default                | Description                      |
|---------------------------------|------------------------|----------------------------------|
| `spring.kafka.bootstrap-servers`| `localhost:9092`       | Kafka broker address             |
| `spring.data.mongodb.uri`       | `mongodb://localhost:27017/event_filter_db` | MongoDB connection |
| `kafka.topic-pattern`           | `events.*`             | Regex pattern for topic subscription |
| `spring.kafka.consumer.group-id`| `event-filter-group`   | Kafka consumer group ID          |

## Monitoring

- **Health Check**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/metrics
- **Kafka UI**: http://localhost:8090
