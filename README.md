# ClickKart Notification Service

Dispatches password-reset links, login OTPs, and contact-verification codes on behalf of other
services. Internal only — never reachable through the API Gateway.

- **Port:** `8082`
- **Datastore:** PostgreSQL (`clickkart_notification`)
- **Callers:** Auth Service, via a Feign client wrapped in a Resilience4j circuit breaker

## ⚠️ Delivery is simulated

This service does **not** integrate with a real SMTP or SMS provider. A "dispatch" writes the
message to a dedicated `DISPATCH` logger (routed to `logs/dispatch.log`) and persists a durable
record. Nothing is actually sent.

That is a deliberate design decision, not an oversight — it keeps the platform runnable without
third-party credentials. Swapping in a real provider means replacing the body of
`NotificationDispatchServiceImpl`; the API contract and persistence layer stay unchanged.

**Consequence:** in any environment using this service, password-reset emails never arrive. Read
the token from the dispatch log instead:

```bash
docker logs clickkart-notification-service | grep SIMULATED_DISPATCH
```

## Endpoints

Both require an `X-Correlation-Id` header — this service never mints one (Auth Service is the
platform's only correlation-ID minter); a missing header is rejected with `400`.

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/v1/notifications/password-reset` | Send a reset token to an email address |
| `POST` | `/api/v1/notifications/otp` | Send an OTP over `EMAIL` or `SMS` |

## What is and isn't persisted

`NotificationEntity` records recipient, channel, type, status, and correlation ID — **never the
raw token or OTP**. Those exist only in the dispatch log line and in the request itself. Storing
them would defeat the point of Auth Service hashing them before persistence.

## Configuration

| Variable | Required in | Notes |
|---|---|---|
| `DB_HOST` | prod | Managed Postgres endpoint |
| `DB_USERNAME` | — | Defaults to `clickkart_notification_app`, this service's own least-privilege role |
| `DB_PASSWORD` | **all** | No default on any profile, dev included |
| `EUREKA_DASHBOARD_USERNAME` / `_PASSWORD` | test/qa/prod | |
| `NOTIFICATION_SERVICE_HOSTNAME` | prod | Eureka advertise address |
| `CONFIG_SERVER_PASSWORD` | test/qa/prod | |

The database role owns only `clickkart_notification` and has `CONNECT` revoked on the other
services' databases — a leaked credential here cannot reach their data.

## Running locally

```bash
docker compose -f docker-compose.dev-infra.yml -f docker-compose.app-tier.yml up -d
curl http://localhost:8082/actuator/health
```

API docs: <http://localhost:8082/swagger-ui.html>, or via the Gateway's aggregated UI at
<http://localhost:8080/swagger-ui.html>.

## Build

```bash
mvn -B verify
```

`verify` enforces the jacoco coverage gate in `pom.xml`, which CI runs on every push.

## Related

- [clickkart-platform](https://github.com/kripals1199/clickkart-platform) — architecture, local setup
- [clickkart-auth-service](https://github.com/kripals1199/clickkart-auth-service) — the only caller
