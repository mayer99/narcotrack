package com.mayer99.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;
import io.socket.client.IO;
import io.socket.client.Socket;
import org.json.JSONObject;

import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

public class SocketAppender extends AppenderBase<ILoggingEvent> {

    private Level level;
    public static boolean disabled;
    private boolean guard;
    private Socket socket;

    public SocketAppender() {
        addInfo("Initializing SocketAppender");
        System.out.println("Initializing  SocketAppender");
        try {
            String authURLString = loadEnvironmentVariable("BACKEND_AUTH_URL");
            String logURLString = loadEnvironmentVariable("BACKEND_LOG_URL");
            String logLevel = loadEnvironmentVariable("BACKEND_LOGLEVEL");
            HttpClient httpClient = HttpClient
                    .newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest authRequest = HttpRequest.newBuilder()
                    .uri(URI.create(authURLString))
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> authResponse = httpClient.send(authRequest, HttpResponse.BodyHandlers.ofString());
            if (authResponse.statusCode() != HttpURLConnection.HTTP_OK) throw new Exception("authResponse returned a statusCode of " + authResponse.statusCode() + ". Body of the result: " + authResponse.body());
            addInfo(String.format("authResponse returned a statusCode of %s, accessToken: %s", authResponse.statusCode(), authResponse.body()));
            level = Level.valueOf(logLevel);
            IO.Options options = IO.Options.builder()
                    .setAuth(Collections.singletonMap("token", authResponse.body()))
                    .build();
            socket = IO.socket(URI.create(logURLString), options);
            socket.connect();
            System.out.println("Reached end of socketAppender");
            disabled = false;


        } catch (Exception e) {
            addWarn("Could not initialize SocketAppender", e);
            disabled = true;
        }
    }

    @Override
    protected void append(ILoggingEvent event) {

        if (level.toInt() > event.getLevel().toInt() || guard || disabled) {
            return;
        }
        guard = true;
        try {
            JSONObject log = new JSONObject();
            log.put("thread", event.getThreadName());
            log.put("level", event.getLevel().toString());
            log.put("message", event.getFormattedMessage());
            log.put("logger", event.getLoggerName());

            if (event.getLevel().toInt() > Level.INFO.toInt()) {
                if (event.getThrowableProxy() != null) {
                    JSONObject exception = new JSONObject();
                    if (event.getThrowableProxy().getMessage() != null && !event.getThrowableProxy().getMessage().trim().isEmpty()) {
                        exception.put("message", event.getThrowableProxy().getMessage());
                    }
                    if (event.getThrowableProxy().getStackTraceElementProxyArray() != null) {
                        exception.put("stacktrace", Arrays.stream(event.getThrowableProxy().getStackTraceElementProxyArray()).map(StackTraceElementProxy::getStackTraceElement).filter(Objects::nonNull).limit(10).map(StackTraceElement::toString).toArray(String[]::new));
                    }
                    if (exception.length() > 0) {
                        log.put("exception", exception);
                    }
                }
            }
            socket.emit("log", log);
        } catch (Exception e) {
            addWarn("Unable to send log event", e);
        }
        guard = false;
    }

    public static String loadEnvironmentVariable(String name) throws Exception {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) throw new Exception(String.format("Could not load %s. Are environment variables available to the execution?", name));
        return value;
    }
}
