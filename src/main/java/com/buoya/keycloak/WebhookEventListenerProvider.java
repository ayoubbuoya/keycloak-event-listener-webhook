package com.buoya.keycloak;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AuthDetails;

public class WebhookEventListenerProvider implements EventListenerProvider {

    private static final Logger logger = Logger.getLogger(WebhookEventListenerProvider.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

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
        sendWebhook(WebhookUserEventPayload.from(event));
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        sendWebhook(WebhookAdminEventPayload.from(event, includeRepresentation));
    }

    private void sendWebhook(Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
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

record WebhookUserEventPayload(
        String eventCategory,
        boolean adminEvent,
        String type,
        String realmId,
        String clientId,
        String userId,
        String sessionId,
        String ipAddress,
        long time,
        String error,
        Map<String, String> details) {

    static WebhookUserEventPayload from(Event event) {
        return new WebhookUserEventPayload(
                "USER",
                false,
                event.getType() != null ? event.getType().name() : "UNKNOWN",
                event.getRealmId(),
                event.getClientId(),
                event.getUserId(),
                event.getSessionId(),
                event.getIpAddress(),
                event.getTime(),
                event.getError(),
                event.getDetails() != null ? event.getDetails() : Collections.emptyMap());
    }
}

record WebhookAdminEventPayload(
        String eventCategory,
        boolean adminEvent,
        String operationType,
        String resourceType,
        String resourcePath,
        String realmId,
        String clientId,
        String userId,
        String ipAddress,
        long time,
        String error,
        String representation) {

    static WebhookAdminEventPayload from(AdminEvent event, boolean includeRepresentation) {
        AuthDetails authDetails = event.getAuthDetails();

        return new WebhookAdminEventPayload(
                "ADMIN",
                true,
                event.getOperationType() != null ? event.getOperationType().name() : "UNKNOWN",
                event.getResourceType() != null ? event.getResourceType().name() : "UNKNOWN",
                event.getResourcePath(),
                authDetails != null ? authDetails.getRealmId() : null,
                authDetails != null ? authDetails.getClientId() : null,
                authDetails != null ? authDetails.getUserId() : null,
                authDetails != null ? authDetails.getIpAddress() : null,
                event.getTime(),
                event.getError(),
                includeRepresentation ? event.getRepresentation() : null);
    }
}
