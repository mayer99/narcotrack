package com.mayer.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;
import io.socket.client.IO;
import io.socket.client.Socket;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

public class SocketAppender extends AppenderBase<ILoggingEvent> {

    private final String authURLString;
    private final String logURLString;
    private final String logLevel;
    private int minimumLevel;
    private boolean active;
    private boolean guard;
    private Socket socket;

    public SocketAppender() {
        authURLString = System.getenv("NARCOTRACK_AUTH_URL");
        logURLString = System.getenv("NARCOTRACK_LOG_URL");
        logLevel = System.getenv("NARCOTRACK_LOGLEVEL");
        if (authURLString == null || authURLString.trim().isEmpty()) {
            addWarn("Could not load authURLString, is the environment variable set and accessible?");
            active = false;
            return;
        }
        if (logURLString == null || logURLString.trim().isEmpty()) {
            addWarn("Could not load logURLString, is the environment variable set and accessible?");
            active = false;
            return;
        }
        if (logLevel == null || logLevel.trim().isEmpty()) {
            addWarn("Could not load logLevel, is the environment variable set and accessible?");
        }
        try {
            URL url = new URL(authURLString);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            if (con.getResponseCode()  != HttpURLConnection.HTTP_OK) {
                addError("Received wrong HTTP code from auth backend: " + collectResponse(con.getErrorStream()));
                active = false;
                return;
            }
            String jwt = collectResponse(con.getInputStream());
            con.disconnect();
            addInfo("Received JWT: " + jwt);

            switch(logLevel) {
                case "ERROR":
                    minimumLevel = Level.ERROR.toInt();
                    break;
                case "WARN":
                    minimumLevel = Level.WARN.toInt();
                    break;
                case "DEBUG":
                    minimumLevel = Level.DEBUG.toInt();
                    break;
                case "INFO":
                default:
                    minimumLevel = Level.INFO.toInt();
            }
            addInfo("LogLevel has been set to " + logLevel + "(" + logLevel + ")");

            URI uri = URI.create(logURLString);
            IO.Options options = IO.Options.builder()
                    .setAuth(Collections.singletonMap("token", jwt))
                    .build();

            socket = IO.socket(uri, options);
            socket.connect();

            active = true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            addError("Could not initialize socket appender", e);
            active = false;
            return;
        }
    }

    @Override
    protected void append(ILoggingEvent event) {

        if (minimumLevel > event.getLevel().toInt()) {
            return;
        }

        if (guard || !active) {
            return;
        }
        guard = true;
        // Die müssen alle geprüft werden, ob throwableProxy existiert
        addInfo(event.getThrowableProxy().getMessage());
        addInfo(event.getThrowableProxy().getClassName());
        String stackTrace = "";
        if (event.getThrowableProxy() != null && event.getThrowableProxy().getStackTraceElementProxyArray() != null) {
            stackTrace = Arrays.stream(event.getThrowableProxy().getStackTraceElementProxyArray()).map(stackTraceElement -> stackTraceElement.getStackTraceElement().toString()).limit(5).collect(Collectors.joining("<br>"));
            addInfo("Stacktrace: " + stackTrace);
        }
        JSONObject eventData = new JSONObject();
        try {
            eventData
                    .put("thread", event.getThreadName())
                    .put("level", event.getLevel().toString())
                    .put("message", event.getFormattedMessage())
                    .put("project", "narcotrack")
                    .put("logger", event.getLoggerName());
            socket.emit("log", eventData);
        } catch (JSONException e) {
            addWarn("Unable to send log event", e);
        }
        guard = false;
    }

    private static String collectResponse(InputStream inputStream) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        return content.toString();
    }
}
