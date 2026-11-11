# Notification Platform

Scalable multi-channel notification system (Email, SMS, Push, In-App, Webhook) built as Spring Boot 3.3 /
Java 17 microservices on Kafka, Redis and PostgreSQL.

## Architecture

```
client --REST--> notification-api-service (8081)  --outbox--> Kafka notif.requests.{high|medium|batch}
                                                                      |
                              preference-service (8082) <--REST-- dispatcher-service (8084)
                                                                      |  opt-out, schedule, quiet hours,
                                                                      |  rate limit (Redis), digest, defer
                                                                      v
                                              Kafka notif.channel.<channel>.{high|std}
                                                                      |
 template-service (8083) <--REST-- channel-workers-service (8085) --> vendors (SendGrid>SES, Twilio>Infobip, FCM,
                                    retry topics + DLT, circuit breakers, idempotency)  WebSocket, webhooks)
                                                                      |
                                                  Kafka notif.events.status
                                                                      v
                                              analytics-tracker-service (8086)  --> delivery_audit_log
```

| Module | Port | Role |
|---|---|---|
| notify-common | - | Shared enums, events, `PreferenceView`, topic names |
| notification-api-service | 8081 | Validation, idempotency, transactional outbox, status API |
| preference-service | 8082 | Opt-outs, quiet hours, time zones, frequency caps, contact addresses |
| template-service | 8083 | Versioned i18n templates rendered with Thymeleaf |
| dispatcher-service | 8084 | Policy pipeline, rate limiting, digest engine, deferral |
| channel-workers-service | 8085 | Channel handlers, vendor failover, retries, DLT, idempotency |
| analytics-tracker-service | 8086 | Delivery state machine, vendor callbacks, audit trail |

## Prerequisites

Java 17+, Maven 3.9+, Docker (for Postgres, Redis and Kafka).

## Quick start

```bash
docker compose up -d          # Postgres, Redis, Kafka (KRaft)
./run-all.sh                  # builds and starts all six services (logs in ./logs)
```

Or run one service at a time: `mvn -pl dispatcher-service spring-boot:run`.

By default the workers use **mock providers** (they log instead of calling vendors), so no credentials are needed.
A demo user `demo-user` and starter templates (`otp`, `welcome`, `marketing.promo`, `social.like`) are seeded on startup.

### Try it

```bash
# High-priority OTP over three channels
curl -s -X POST localhost:8081/v1/notifications \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: otp-001' \
  -d '{"userId":"demo-user","templateKey":"otp","priority":"HIGH","category":"OTP",
       "channels":["SMS","EMAIL","IN_APP","WEBHOOK"],"locale":"en",
       "variables":{"code":"482913","minutes":5}}'

# Hindi localisation
curl -s -X POST localhost:8081/v1/notifications -H 'Content-Type: application/json' -H 'Idempotency-Key: otp-002' \
  -d '{"userId":"demo-user","templateKey":"otp","priority":"HIGH","category":"OTP","channels":["SMS"],"locale":"hi","variables":{"code":"111222","minutes":5}}'

# Poll delivery state (use the notificationId returned above)
curl -s localhost:8081/v1/notifications/<notificationId>

# Idempotency: repeat the first call with the same Idempotency-Key and you get the same notificationId back.

# Digest: send several, they collapse into one "N people liked your comment" after the window
for i in 1 2 3 4 5; do curl -s -X POST localhost:8081/v1/notifications -H 'Content-Type: application/json' \
  -H "Idempotency-Key: like-$i" -d "{\"userId\":\"demo-user\",\"templateKey\":\"social.like\",\"priority\":\"MEDIUM\",
  \"category\":\"SOCIAL_DIGEST\",\"channels\":[\"PUSH\"],\"variables\":{\"actorName\":\"User$i\"}}"; done
# Shorten the window for a quick demo: NOTIFY_DIGEST_WINDOW=PT30S mvn -pl dispatcher-service spring-boot:run

# Preferences: opt the demo user out of marketing email, or set quiet hours
curl -X PUT localhost:8082/internal/preferences -H 'Content-Type: application/json' \
  -d '{"userId":"demo-user","channel":"EMAIL","category":"MARKETING","enabled":true,
       "quietStart":"22:00","quietEnd":"07:00","timeZone":"Asia/Kolkata","maxPerHour":5,"preferredLocale":"en"}'
```

Watch retries and dead letters by simulating vendor outages:
`NOTIFY_PROVIDERS_MOCK_FAILURE_RATE=0.7 mvn -pl channel-workers-service spring-boot:run`.

## Going live with real vendors

Set `notify.providers.mode=live` (env `NOTIFY_PROVIDERS_MODE=live`) on channel-workers-service and provide:

| Variable | Used by |
|---|---|
| `SENDGRID_API_KEY`, `EMAIL_FROM` | SendGrid (primary email) |
| AWS credentials and `AWS_REGION` | SES (email failover) |
| `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM`, `TWILIO_CALLBACK_BASE_URL` | Twilio (primary SMS) |
| `INFOBIP_BASE_URL`, `INFOBIP_API_KEY`, `INFOBIP_SENDER` | Infobip (SMS failover) |
| `GOOGLE_APPLICATION_CREDENTIALS` | FCM push (Android, web, iOS via APNs) |
| `WEBHOOK_SIGNING_SECRET` | HMAC signing of webhook payloads |

Failover order is set with `@Order` on the providers; each is wrapped in a Resilience4j circuit breaker
(tune under `resilience4j.*` in `channel-workers-service/src/main/resources/application.yml`).
On the tracker, set `notify.twilio.verify-signature=true` so Twilio callbacks are authenticated.

## Reliability design in this code

- **No lost requests:** the API writes the request and an outbox row in one transaction; a relay publishes to Kafka (`acks=all`, idempotent producer).
- **At-least-once + dedup:** workers are idempotent via a Redis `PROCESSING`/`DONE` marker; duplicates already done are skipped, in-flight duplicates retry later.
- **Priority isolation:** separate request and channel topics for HIGH traffic, and a dedicated consumer pool in the dispatcher.
- **Retries:** non-blocking retry topics with exponential backoff and jitter, then a dead-letter topic (`@RetryableTopic`).
- **Critical bypass:** OTP and SECURITY skip opt-outs, quiet hours and frequency caps.
- **State machine:** `PENDING -> PROCESSING -> SENT -> DELIVERED -> READ`, or `FAILED`; stale and duplicate events are ignored.

## Tests

`mvn test` runs unit tests for the state machine, preference rules (quiet hours across midnight, caps),
vendor failover (`ProviderRouter`) and the tracker's transition handling.

## Known limits and next steps

- **Build status:** `notify-common` was compiled and behaviour-checked, and every module passed static checks, but the Spring modules
  could not be built in the environment that generated this project (no Maven Central access). Run `mvn clean verify` first and
  expect to fix small compile or version issues.
- No authentication or authorization on any endpoint; put the API behind a gateway and secure `/internal/**`.
- In-app delivery uses Spring's in-memory STOMP broker (single pod). Use a broker relay or Redis pub/sub to fan out across pods.
- The audit trail uses PostgreSQL; switch to Cassandra (partition by notification id) plus Elasticsearch for very high volume.
- Kafka partition and replica counts are laptop-sized; raise them via `notify.kafka.*` (see section 2.3 of the design doc).
- Preference and contact lookups are cached for 60 s in the dispatcher; evict on change via a Kafka event if you need instant effect.
- Add Micrometer/Prometheus, tracing, and KEDA autoscaling on consumer lag for production.
