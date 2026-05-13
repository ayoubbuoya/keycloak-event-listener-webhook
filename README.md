# Keycloak Webhook Event Listener

A Keycloak SPI event listener provider that forwards user authentication and authorization events to a configurable webhook endpoint in real time.

## Features

- Forwards Keycloak user events as JSON via HTTP POST
- Asynchronous, non-blocking webhook delivery using Java HttpClient
- Configurable webhook URL per realm
- Zero external runtime dependencies (uses only Keycloak and JDK APIs)
- Custom JSON serialization (no Jackson/Gson required)

## Requirements

- **Java**: 21+
- **Keycloak**: 26.6.1

## Webhook Payload

Each event is sent as a JSON POST request with `Content-Type: application/json`:

```json
{
  "type": "LOGIN",
  "realmId": "my-realm",
  "clientId": "my-client",
  "userId": "f:abc123:user-uuid",
  "sessionId": "session-uuid",
  "ipAddress": "192.168.1.1",
  "time": 1715600000000,
  "error": "",
  "details": {
    "username": "john.doe"
  }
}
```

| Field       | Type   | Description                        |
| ----------- | ------ | ---------------------------------- |
| `type`      | string | Keycloak event type (e.g. `LOGIN`) |
| `realmId`   | string | Realm where the event occurred     |
| `clientId`  | string | Client that triggered the event    |
| `userId`    | string | User ID associated with the event  |
| `sessionId` | string | Session ID                         |
| `ipAddress` | string | Client IP address                  |
| `time`      | long   | Event timestamp (epoch millis)     |
| `error`     | string | Error message, empty if success    |
| `details`   | object | Additional event details           |

## Build

```bash
mvn clean package
```

The JAR is generated at `target/keycloak-event-listener-1.0.jar`.

## Install

1. Copy the built JAR into your Keycloak `providers/` directory:

   ```bash
   cp target/keycloak-event-listener-1.0.jar <keycloak-home>/providers/
   ```

2. Rebuild Keycloak's provider registry:

   ```bash
   bin/kc.[sh|bat] build
   ```

3. Restart Keycloak.

## Configure

### Default Webhook URL

The provider uses a default webhook URL:

```
http://localhost:8081/api/v1/keycloak/webhook
```

### Custom Webhook URL

Set the `webhookUrl` parameter via Keycloak CLI or environment variable:

**CLI:**

```bash
bin/kc.[sh|bat] start --spi-events-listener-webhook-event-listener-webhook-url=https://your-server.com/api/webhook
```

**Environment variable:**

```bash
KC_SPI_EVENTS_LISTENER_WEBHOOK_EVENT_LISTENER_WEBHOOK_URL=https://your-server.com/api/webhook
```

### Enable in Realm

1. Navigate to **Realm Settings** > **Events**
2. Under **Event Listeners**, add `webhook-event-listener`
3. Save

## Architecture

```
WebhookEventListenerProviderFactory   (SPI discovery & config)
  └── WebhookEventListenerProvider    (event handling & HTTP delivery)
```

The provider uses Keycloak's [Service Provider Interface](https://www.keycloak.org/docs/latest/server_development/#_providers) pattern:

- **`WebhookEventListenerProviderFactory`** — registered via `META-INF/services/org.keycloak.events.EventListenerProviderFactory`, reads the `webhookUrl` config parameter, and creates provider instances.
- **`WebhookEventListenerProvider`** — implements `EventListenerProvider`, converts Keycloak `Event` objects to JSON, and delivers them asynchronously via `HttpClient.sendAsync()`.

## Project Structure

```
src/main/
├── java/com/buoya/keycloak/
│   ├── WebhookEventListenerProvider.java       # Event handling & webhook delivery
│   └── WebhookEventListenerProviderFactory.java # Factory & configuration
└── resources/META-INF/services/
    └── org.keycloak.events.EventListenerProviderFactory
```
