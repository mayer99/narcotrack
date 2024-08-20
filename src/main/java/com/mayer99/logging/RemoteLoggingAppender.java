package com.mayer99.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Properties;

public class RemoteLoggingAppender extends AppenderBase<ILoggingEvent> {
    private final HttpClient client;
    private final Properties properties = new Properties();
    private String accessToken;
    private boolean disabled = true;
    private boolean guard;

    public RemoteLoggingAppender() {
        client = HttpClient
                .newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        try {
            loadConfigFile();
            requestAccessToken();
            disabled = false;
        } catch (JSONException | InterruptedException| SecurityException | IOException | IllegalArgumentException | NullPointerException e) {
            e.printStackTrace();
            System.out.println("Could not start RemoteLoggingAppender");
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
                for (StackTraceElement element : event.getCallerData()) {
                    messages.add(element.toString());
                }
            }

            JSONObject log = new JSONObject();
            log.put("level", event.getLevel().toString().toLowerCase());
            log.put("messages", messages.toArray(String[]::new));
            log.put("created_at", System.currentTimeMillis());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getProperty("LOGGING_ENDPOINT")))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + accessToken)
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(log.toString()))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() != HttpURLConnection.HTTP_CREATED) {
                            addWarn("Failed to send log. Response from Server: " + response.body());
                        }
                    })
                    .exceptionally(e -> {
                        addError("Failed to send log", e);
                        return null;
                    });
        } catch (JSONException e) {
            addError("Could not parse log", e);
        }

        guard = false;
    }

    private void loadConfigFile() throws SecurityException, IOException, IllegalArgumentException, NullPointerException {
        InputStream input = new FileInputStream("src/main/resources/logging.config");
        properties.load(input);
    }

    private void requestAccessToken() throws JSONException, IllegalStateException, IllegalArgumentException, IOException, InterruptedException {
        JSONObject credentials = new JSONObject();
        credentials.put("client_id", getEnvironmentVariable("LOGGING_CLIENT_ID"));
        credentials.put("client_secret", getEnvironmentVariable("LOGGING_CLIENT_SECRET"));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getProperty("AUTH_ENDPOINT")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(credentials.toString()))
                .timeout(Duration.ofSeconds(5))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != HttpURLConnection.HTTP_CREATED) throw new IllegalStateException(response.body());
        JSONObject body = new JSONObject(response.body());
        accessToken = body.getString("access_token");
        if (isNullEmptyOrWhitespace(accessToken)) throw new IllegalArgumentException("accessToken is null, empty or whitespace.");
    }

    private String getProperty(String key) throws IllegalArgumentException {
        String value = properties.getProperty(key);
        if (isNullEmptyOrWhitespace(value)) throw new IllegalArgumentException(String.format("%s is null, empty or whitespace.", key));
        return value;
    }

    private String getEnvironmentVariable(String key) throws IllegalArgumentException {
        String value = System.getenv(key);
        if (isNullEmptyOrWhitespace(value)) throw new IllegalArgumentException(String.format("%s is null, empty or whitespace.", key));
        return value;
    }

    private boolean isNullEmptyOrWhitespace(String value) {
        return value == null || value.trim().isEmpty();
    }
}
