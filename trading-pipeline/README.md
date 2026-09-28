# Trading Pipeline

A runnable Spring Boot 4 / Java 21 reference implementation of the architecture in `trading-pipeline-architecture.md`.

## Flow

- `POST /api/v1/trades` validates a trade and asynchronously publishes a JSON event to Kafka, keyed by `securityId|tradeDate|source`.
- One batch Kafka consumer group bulk-upserts MongoDB and pipelines Redis writes. It acknowledges offsets only after both projections succeed.
- `GET /api/v1/trades/{securityId}?from=YYYY-MM-DD&to=YYYY-MM-DD` queries the MongoDB historical projection.
- MongoDB stores one record per `(securityId, tradeDate, source)` and the application ensures a matching compound unique index at startup.
- Virtual threads are enabled for blocking application I/O. Kafka sends are not wrapped in a separate executor and are not awaited by the HTTP request.

The sample API is the source adapter. File, database, or external API adapters can call `TradePublisher` after transforming their source records. If multiple producers can publish the same key concurrently, source ownership or a monotonic version check is still required; Kafka keying alone does not serialize independent producers.

## Run locally

Prerequisites: Java 21+ and Docker with Compose.

```powershell
docker compose up -d
.\gradlew.bat bootRun
```

The application defaults target the exposed local ports. Override them with environment variables or load the explicit sample configuration:

```powershell
.\gradlew.bat bootRun --args="--spring.config.additional-location=file:samples/application-local.yml"
```

Submit a sample trade (the body is JSON; `samples/trade-request.yml` shows the same fields in YAML):

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/trades `
  -ContentType 'application/json' `
  -Body '{"securityId":"AAPL","tradeDate":"2026-09-25","source":"sample-feed","price":227.45,"quantity":100}'
```

Query the eventually consistent historical projection:

```powershell
Invoke-RestMethod 'http://localhost:8080/api/v1/trades/AAPL?from=2026-09-01&to=2026-09-30'
```

Health is available at `http://localhost:8080/actuator/health`.

## Compose-network configuration

To run the application in the same Docker network as the dependencies, use `samples/application-compose.yml`. For example, set `SPRING_CONFIG_ADDITIONAL_LOCATION=file:samples/application-compose.yml` in the app container. The Compose file intentionally only runs Kafka, MongoDB, and Redis so the Java app can be started and debugged directly from the IDE.

## Build and test

```powershell
.\gradlew.bat clean test bootJar
```

MongoDB and Redis are read projections, not the system of record. A Kafka replay can rebuild them; delivery is at-least-once, with idempotent upserts and Redis sets handling redelivery.
