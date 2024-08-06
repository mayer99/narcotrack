package com.mayer99.narcotrack.handlers;

import com.mayer99.Narcotrack;
import com.mayer99.narcotrack.base.models.NarcotrackEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class CriticalErrorHandler implements NarcotrackEventHandler {

    private static final String WEBHOOK_URL = "NT_WEBHOOK_URL";
    private static final Logger logger = LoggerFactory.getLogger(CriticalErrorHandler.class);

    private boolean disabled;

    private HttpClient httpClient;
    private HttpRequest httpRequest;

    public CriticalErrorHandler() {
        logger.info("CriticalErrorHandler starting...");
        try {
            String webhookUrl = System.getenv(WEBHOOK_URL);
            if (Narcotrack.isNullEmptyOrWhitespace(webhookUrl)) {
                throw new IllegalArgumentException(String.format("The environment variable %s ist not set or has an invalid value. Are the environment variables loaded?", WEBHOOK_URL));
            }
            httpClient = HttpClient
                    .newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            disabled = false;
        } catch (Exception e) {
            disabled = true;
            logger.error("Could not initialize CriticalErrorHandler", e);
        }
    }

    @Override
    public void onCriticalError() {
        if (disabled) {
            logger.error("Could not trigger webhook, handler is disabled");
            return;
        }
        try {
            httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString());
            logger.info("Webhook triggered");
        } catch (Exception e) {
            logger.error("Could not trigger webhook", e);
        }
    }
}
