package com.buoya.keycloak;

import org.keycloak.Config.Scope;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class WebhookEventListenerProviderFactory implements EventListenerProviderFactory {

    private static final String PROVIDER_ID = "webhook-event-listener";
    private static final String DEFAULT_WEBHOOK_URL = "http://localhost:8080/api/v1/keycloak/webhook";

    private String webhookUrl;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new WebhookEventListenerProvider(webhookUrl);
    }

    @Override
    public void init(Scope config) {
        webhookUrl = config.get("webhookUrl", DEFAULT_WEBHOOK_URL);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
