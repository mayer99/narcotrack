package com.mayer99.narcotrack.event.handlers;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.IThrowableProxy;
import com.mayer99.narcotrack.application.NarcotrackApplication;
import com.mayer99.narcotrack.event.NarcotrackEventHandler;
import com.mayer99.narcotrack.event.events.ReceivedRemainsEvent;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

public class RemoteEventHandler implements NarcotrackEventHandler {

    private final Logger LOGGER = LoggerFactory.getLogger(RemoteEventHandler.class);
    private String eventEndpoint;
    private HttpClient client;
    private String accessToken;
    public boolean disabled;

    public RemoteEventHandler() {
        try {
            eventEndpoint = NarcotrackApplication.getEnvironmentVariable("EVENT_LOG_ENDPOINT");
            client = HttpClient
                    .newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            authenticate();
            disabled = false;
        } catch (Exception e) {
            LOGGER.error("Could not initialize RemoteEventHandler", e);
            disabled = true;
        }
    }

    private void authenticate() throws Exception {
        JSONObject credentials = new JSONObject();
        credentials.put("client_id", NarcotrackApplication.getEnvironmentVariable("EVENT_CLIENT_ID"));
        credentials.put("client_secret", NarcotrackApplication.getEnvironmentVariable("EVENT_CLIENT_SECRET"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(NarcotrackApplication.getEnvironmentVariable("EVENT_AUTH_ENDPOINT")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(credentials.toString()))
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != HttpURLConnection.HTTP_CREATED) {
            throw new Exception("Received invalid response from server " + response.statusCode() + " " + response.body());
        }
        JSONObject body = new JSONObject(response.body());
        accessToken = body.getString("access_token");
        if (NarcotrackApplication.isNullEmptyOrWhitespace(accessToken)) {
            throw new Exception("accessToken is null, empty or whitespace");
        }
    }

    private void sendEventMessage(String name) {
        if (disabled) return;
        try {
            JSONObject log = new JSONObject();
            log.put("name", name);
            log.put("created_at", System.currentTimeMillis());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(eventEndpoint))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + accessToken)
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(log.toString()))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() != HttpURLConnection.HTTP_CREATED) {
                            LOGGER.warn("Backend failed to process log");
                            LOGGER.warn("StatusCode: {}", response.statusCode());
                            LOGGER.warn("Response body {}", response.body());
                        }
                    })
                    .exceptionally(e -> {
                        LOGGER.warn("Failed to send event", e);
                        return null;
                    });
        } catch (Exception e) {
            LOGGER.warn("Failed to send log", e);
            LOGGER.warn("Disabling RemoteEventHandler");
            disabled = true;
        }
    }

    @Override
    public void onRecoverableError() {
        if (disabled) return;
        sendEventMessage("recoverable_error");
    }

    @Override
    public void onDetachedElectrode() {
        if (disabled) return;
        sendEventMessage("detached_electrode");
    }

    @Override
    public void onLooseElectrode() {
        if (disabled) return;
        sendEventMessage("loose_electrode");
    }

    @Override
    public void onGoodElectrodes() {
        if (disabled) return;
        sendEventMessage("good_electrodes");
    }

    @Override
    public void onRecordingStart(Instant time) {
        if (disabled) return;
        sendEventMessage("recording_start");
    }

    @Override
    public void onRecordingStop() {
        if (disabled) return;
        sendEventMessage("recording_stop");
    }

    @Override
    public void onHandleRemains(ReceivedRemainsEvent event) {
        if (disabled) return;
        sendEventMessage("handle_remains");
    }

    @Override
    public void onIntervalStart() {
        if (disabled) return;
        sendEventMessage("interval_start");
    }

    @Override
    public void onUnrecoverableError() {
        if (disabled) return;
        sendEventMessage("unrecoverable_error");
    }
}
