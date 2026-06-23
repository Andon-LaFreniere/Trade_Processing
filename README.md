# Trade Processing API

simplified stock brokerage backend

trade lifecycle managment, order matching, portfolio P&L

Built with Apache Kafka, Java 17, Spring Boot 3

## Quickstart

### Prerequisites

- Java 17+
- Maven 3.8+
- (Optional) Apache Kafka on `localhost:9092` for event publishing

### Without Kafka

```bash
git clone https://github.com/Andon-LaFreniere/Trade_Processing.git
cd Trade_Processing
mvn spring-boot:run -Dspring-boot.run.jvmArguments="\
  -Dspring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
```

### With Kafka (Docker)

```bash
# Start Kafka
docker run -d --name kafka -p 9092:9092 \
  -e KAFKA_ENABLE_KRAFT=yes \
  -e KAFKA_CFG_PROCESS_ROLES=broker,controller \
  -e KAFKA_CFG_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093 \
  -e KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT \
  -e KAFKA_CFG_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
  -e KAFKA_CFG_CONTROLLER_LISTENER_NAMES=CONTROLLER \
  bitnami/kafka:latest


##
```

Client Request
│
▼
REST Controllers ──────────────────────────────────────────────────────┐
(AccountController, OrderController, PortfolioController, MarketController)
│
▼
Service Layer
├── AccountService — account creation & balance management
├── OrderService — order submission facade
├── OrderMatchingEngine — core matching logic (MARKET / LIMIT)
├── PortfolioService — position valuation & P&L
└── MarketPriceService — simulated market data
│
├── JPA Repositories (H2 in-memory)
│ ├── accounts — cash balances
│ ├── orders — mutable order state
│ ├── trades — immutable settlement records
│ ├── positions — per-account share holdings
│ └── market_prices — symbol price store
│
└── Kafka Producer ──► [trade-executed topic] ──► TradeAuditConsumer
(compliance logging,
large trade alerts)

```

## Featues

## Tech Stack

## API Reference

## Run Tests

## Design Decisions
```
