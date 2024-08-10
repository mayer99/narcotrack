package com.mayer99.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
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
import java.util.Properties;

public class BackendAppender extends AppenderBase<ILoggingEvent> {

    private final Properties properties = new Properties();
    private HttpClient httpClient;
    private String accessToken;

    private boolean disabled = true;
    private boolean guard;

    public BackendAppender() {
        System.out.println("Loading config file...");
        try (InputStream input = new FileInputStream("logging.config")) {
            properties.load(input);
        } catch (SecurityException | IOException | IllegalArgumentException | NullPointerException e) {
            System.out.println("Could not load configuration file");
            e.printStackTrace();
            return;
        }
        String authURL = properties.getProperty("AUTH_URL");
        String logURL = properties.getProperty("LOG_URL");
        if (authURL == null || authURL.trim().isEmpty()) {
            System.out.println("AUTH_URL is null, empty or whitespace. Please check the configuration file");
            return;
        }
        if (logURL == null || logURL.trim().isEmpty()) {
            System.out.println("LOG_URL is null, empty or whitespace. Please check the configuration file");
            return;
        }
        String clientId = System.getenv("BACKEND_CLIENT_ID");
        String clientSecret = System.getenv("BACKEND_CLIENT_SECRET");
        if (clientId == null || clientId.trim().isEmpty()) {
            System.out.println("BACKEND_CLIENT_ID is null, empty or whitespace. Please check the environment variables");
            return;
        }
        if (clientSecret == null || clientSecret.trim().isEmpty()) {
            System.out.println("BACKEND_CLIENT_SECRET is null, empty or whitespace. Please check the environment variables");
            return;
        }
        try {
            httpClient = HttpClient
                    .newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            JSONObject credentials = new JSONObject();
            credentials.put("clientId", clientId);
            credentials.put("clientSecret", clientSecret);
            HttpRequest authRequest = HttpRequest.newBuilder()
                    .uri(URI.create(authURL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(credentials.toString()))
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> authResponse = httpClient.send(authRequest, HttpResponse.BodyHandlers.ofString());

            if (authResponse.statusCode() != HttpURLConnection.HTTP_CREATED) {
                System.out.println("Could not authenticate");
                System.out.println(authResponse.body());
                return;
            }
            JSONObject authResponseBody = new JSONObject(authResponse.body());
            accessToken = authResponseBody.getString("access_token");
            disabled = false;
        } catch (Exception e) {
            System.out.println("Could not start BackendAppender");
            e.printStackTrace();
        }
    }

    @Override
    protected void append(ILoggingEvent event) {

        if (guard || disabled || event.getLevel().toInt() < Level.DEBUG.toInt()) {
            return;
        }
        guard = true;
        JSONObject log = new JSONObject();
        try {
            log.put("level", event.getLevel().toString().toLowerCase());
            log.put("message", event.getFormattedMessage());
            log.put("created_at", System.currentTimeMillis());

            HttpRequest logRequest = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getProperty("LOG_URL")))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + accessToken)
                    .POST(HttpRequest.BodyPublishers.ofString(log.toString()))
                    .timeout(Duration.ofSeconds(5))
                    .build();

            httpClient.sendAsync(logRequest, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                            System.out.println("Failed to send log: " + response.body());
                        }
                    })
                    .exceptionally(e -> {
                        System.out.println("Error sending log");
                        e.printStackTrace();
                        return null;
                    });
        } catch (JSONException e) {
            System.out.println("Could not create log object");
            e.printStackTrace();
        }

        guard = false;
    }
}
