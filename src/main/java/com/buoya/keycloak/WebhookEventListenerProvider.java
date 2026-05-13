package com.buoya.keycloak;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.StringJoiner;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;

public class WebhookEventListenerProvider implements EventListenerProvider {

    private static final Logger logger = Logger.getLogger(WebhookEventListenerProvider.class);

    private final HttpClient httpClient;
    private final String webhookUrl;

    public WebhookEventListenerProvider(String webhookUrl) {
        this.webhookUrl = webhookUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public void onEvent(Event event) {
        sendWebhook(toJson(event));
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // Not forwarding admin events — add if needed
    }

    private String toJson(Event event) {
        StringJoiner sj = new StringJoiner(",", "{", "}");
        sj.add("\"type\":\"" + (event.getType() != null ? event.getType().name() : "UNKNOWN") + "\"");
        sj.add("\"realmId\":\"" + escape(event.getRealmId()) + "\"");
        sj.add("\"clientId\":\"" + escape(event.getClientId()) + "\"");
        sj.add("\"userId\":\"" + escape(event.getUserId()) + "\"");
        sj.add("\"sessionId\":\"" + escape(event.getSessionId()) + "\"");
        sj.add("\"ipAddress\":\"" + escape(event.getIpAddress()) + "\"");
        sj.add("\"time\":" + event.getTime());
        sj.add("\"error\":\"" + escape(event.getError()) + "\"");
        sj.add("\"details\":" + mapToJson(event.getDetails()));
        return sj.toString();
    }

    private String mapToJson(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        StringJoiner sj = new StringJoiner(",", "{", "}");
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sj.add("\"" + escape(entry.getKey()) + "\":\"" + escape(entry.getValue()) + "\"");
        }
        return sj.toString();
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void sendWebhook(String json) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() >= 300) {
                            logger.warnf("Webhook returned status %d", response.statusCode());
                        }
                    })
                    .exceptionally(ex -> {
                        logger.warnf("Webhook call failed: %s", ex.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            logger.warnf("Failed to send webhook: %s", e.getMessage());
        }
    }

    @Override
    public void close() {
    }
}
