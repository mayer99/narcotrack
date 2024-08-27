package com.mayer99.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import org.json.JSONObject;

import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;

public class RemoteLoggingAppender extends AppenderBase<ILoggingEvent> {
    private String logEndpoint;
    private HttpClient client;
    private String accessToken;
    public static boolean disabled;
    private boolean guard;

    public RemoteLoggingAppender() {
        try {
            logEndpoint = getEnvironmentVariable("LOGGING_LOG_ENDPOINT");
            client = HttpClient
                    .newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            authenticate();
            disabled = false;
        } catch (Exception e) {
            System.out.println("Could not initialize RemoteLoggingAppender");
            e.printStackTrace();
            disabled = true;
        }
    }

    private void authenticate() throws Exception {
        JSONObject credentials = new JSONObject();
        credentials.put("client_id", getEnvironmentVariable("LOGGING_CLIENT_ID"));
        credentials.put("client_secret", getEnvironmentVariable("LOGGING_CLIENT_SECRET"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getEnvironmentVariable("LOGGING_AUTH_ENDPOINT")))
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
        if (isNullEmptyOrWhitespace(accessToken)) {
            throw new Exception("accessToken is null, empty or whitespace");
        }
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (guard || disabled || !event.getLevel().isGreaterOrEqual(Level.DEBUG)) return;
        guard = true;
        try {
            ArrayList<String> messages = new ArrayList<>();
            messages.add(event.getFormattedMessage());

            if (event.getThrowableProxy() != null) {
                IThrowableProxy throwableProxy = event.getThrowableProxy();
                messages.add(String.format("%s: %s", throwableProxy.getClassName(), throwableProxy.getMessage()));
                StackTraceElement[] callerData = event.getCallerData();
                if (callerData.length > 10) {
                    for (int i = 0; i < 10; i++) {
                        messages.add(callerData[i].toString());
                    }
                    messages.add("...");
                } else {
                    for (StackTraceElement element : callerData) {
                        messages.add(element.toString());
                    }
                }
            }

            JSONObject log = new JSONObject();
            log.put("level", event.getLevel().toString().toLowerCase());
            log.put("messages", messages.toArray(String[]::new));
            log.put("created_at", System.currentTimeMillis());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(logEndpoint))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + accessToken)
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(log.toString()))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() != HttpURLConnection.HTTP_CREATED) {
                            addWarn("Backend failed to process log");
                            addWarn("StatusCode: " + response.statusCode());
                            addWarn("Response body: " + response.body());
                        }
                    })
                    .exceptionally(e -> {
                        addError("Failed to send log", e);
                        return null;
                    });
        } catch (Exception e) {
            addWarn("Failed to send log", e);
        }
        guard = false;
    }

    private String getEnvironmentVariable(String key) throws IllegalArgumentException, NullPointerException {
        String value = System.getenv(key);
        if (isNullEmptyOrWhitespace(value)) throw new IllegalArgumentException(String.format("%s is null, empty or whitespace.", key));
        return value;
    }

    private boolean isNullEmptyOrWhitespace(String value) {
        return value == null || value.trim().isEmpty();
    }

}
