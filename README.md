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
```

## Architecture

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

Order Types - MARKET & LIMIT

Trade Lifecycle - Submit → Validate → Match → Settle → Publish event

Portfolio P&L - Real-time unrealized P&L and weighted average cost nasis

Order Book - Pending limit order visible

## Tech Stack

- **Java 17** / **Spring Boot 3.2**
- **Spring Data JPA** + **H2** (swap to PostgreSQL for production)
- **Apache Kafka** — event-driven trade settlement notifications
- **springdoc-openapi** — Swagger UI
- **Lombok** — boilerplate reduction
- **JUnit 5** + **Mockito** — unit and integration tests

## API Reference

### Accounts

| Method | Endpoint             | Description         |
| ------ | -------------------- | ------------------- |
| `POST` | `/api/accounts`      | Open a new account  |
| `GET`  | `/api/accounts/{id}` | Get account details |
| `GET`  | `/api/accounts`      | List all accounts   |

### Orders

| Method   | Endpoint                           | Description                |
| -------- | ---------------------------------- | -------------------------- |
| `POST`   | `/api/orders`                      | Submit a BUY or SELL order |
| `DELETE` | `/api/orders/{id}?accountId=`      | Cancel a pending order     |
| `GET`    | `/api/orders/account/{id}`         | Order history              |
| `GET`    | `/api/orders/account/{id}/pending` | Pending limit orders       |

### Portfolio

| Method | Endpoint                            | Description                 |
| ------ | ----------------------------------- | --------------------------- |
| `GET`  | `/api/portfolio/{accountId}`        | Portfolio snapshot with P&L |
| `GET`  | `/api/portfolio/{accountId}/trades` | Trade history               |

### Market

| Method | Endpoint                         | Description                               |
| ------ | -------------------------------- | ----------------------------------------- |
| `GET`  | `/api/market/prices`             | All symbol prices                         |
| `PUT`  | `/api/market/prices/{symbol}`    | Update price (triggers limit order sweep) |
| `GET`  | `/api/market/orderbook/{symbol}` | Pending bids and asks                     |

## Run Tests

```bash
mvn test
```

Unit tests cover the core matching engine (market orders, limit order queuing, insufficient funds rejection, weighted average cost). Integration tests cover REST API contract validation.

## Design Decisions

I seperated order and trade entities because orders are mutable whereas trades are immutable settlement records. This is like a real financial system where the order book is a source of truth for regulators.

I chose to use Kafka because financial systems are inherently event-driven. Kafka seperates consumer actions from the matching engine so that they can scale independently.

Buying additional shares updates the position's average cost using the standard cost basis method for brokerage accounts: `(existingShares × avgCost + newShares × fillPrice) / totalShares`
