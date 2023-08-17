package com.mayer.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
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

    private int minimumLevel;
    private boolean active;
    private boolean guard;
    private Socket socket;

    public SocketAppender() {

        try {
            String authURLString = System.getenv("NARCOTRACK_AUTH_URL");
            String logURLString = System.getenv("NARCOTRACK_LOG_URL");
            String logLevel = System.getenv("NARCOTRACK_LOGLEVEL");
            if (authURLString == null || authURLString.trim().isEmpty()) throw new Exception("Could not load authURLString, is the environment variable set and accessible?");
            if (logURLString == null || logURLString.trim().isEmpty()) throw new Exception("Could not load logURLString, is the environment variable set and accessible?");
            if (logLevel == null || logLevel.trim().isEmpty()) {
                addWarn("Could not load logLevel, is the environment variable set and accessible?");
                logLevel = "";
            }
            URL url = new URL(authURLString);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            if (con.getResponseCode()  != HttpURLConnection.HTTP_OK) throw new Exception("Received wrong HTTP code from auth backend: " + collectResponse(con.getErrorStream()));

            String jwt = collectResponse(con.getInputStream());
            con.disconnect();
            addInfo("Received JWT: " + jwt);

            switch (logLevel) {
                case "ERROR" -> minimumLevel = Level.ERROR.toInt();
                case "WARN" -> minimumLevel = Level.WARN.toInt();
                case "DEBUG" -> minimumLevel = Level.DEBUG.toInt();
                default -> minimumLevel = Level.INFO.toInt();
            }

            URI uri = URI.create(logURLString);
            IO.Options options = IO.Options.builder()
                    .setAuth(Collections.singletonMap("token", jwt))
                    .build();
            socket = IO.socket(uri, options);
            socket.connect();

            active = true;
        } catch (Exception e) {
            addWarn("Could not initialize SocketAppender", e);
            active = false;
        }
    }

    @Override
    protected void append(ILoggingEvent event) {

        if (minimumLevel > event.getLevel().toInt() || guard || !active) {
            return;
        }
        guard = true;

        try {
            JSONObject data = new JSONObject();
            data
                    .put("thread", event.getThreadName())
                    .put("level", event.getLevel().toString())
                    .put("message", event.getFormattedMessage())
                    .put("project", "narcotrack")
                    .put("logger", event.getLoggerName());

            if (event.getLevel().toInt() > Level.INFO.toInt() && event.getThrowableProxy() != null) {
                JSONObject exceptionData = new JSONObject();
                if (event.getThrowableProxy().getMessage() != null && !event.getThrowableProxy().getMessage().trim().isEmpty()) {
                    exceptionData.put("message", event.getThrowableProxy().getMessage().trim());
                }
                if (event.getThrowableProxy().getStackTraceElementProxyArray() != null) {
                    exceptionData.put("stackTrace", Arrays.stream(event.getThrowableProxy().getStackTraceElementProxyArray()).filter(stackTraceElementProxy -> stackTraceElementProxy.getStackTraceElement() != null).limit(10).map(stackTraceElementProxy -> stackTraceElementProxy.getStackTraceElement().toString()).collect(Collectors.joining("<br>")));
                }
                if (exceptionData.length() > 0) {
                    data.put("exception", exceptionData);
                }
            }
            socket.emit("log", data);
        } catch (Exception e) {
            addWarn("Unable to send log event", e);
        }
        guard = false;
    }

    private static String collectResponse(InputStream inputStream) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        return content.toString();
    }
}
