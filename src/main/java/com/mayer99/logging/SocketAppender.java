package com.mayer99.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import io.socket.client.IO;
import io.socket.client.Socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Collections;

public class SocketAppender extends AppenderBase<ILoggingEvent> {

    private int minimumLevel;
    private boolean active;
    private boolean guard;
    private Socket socket;
    private Log.LogBuilder logBuilder;

    public SocketAppender() {

        try {
            String authURLString = System.getenv("BACKEND_AUTH_URL");
            String logURLString = System.getenv("BACKEND_LOG_URL");
            String logLevel = System.getenv("BACKEND_LOGLEVEL");
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
            if (con.getResponseCode() != HttpURLConnection.HTTP_OK) throw new Exception("Received wrong HTTP code from auth backend: " + collectResponse(con.getErrorStream()));

            String response = collectResponse(con.getInputStream());
            con.disconnect();
            addInfo("Received response: " + response);

            switch (logLevel) {
                case "ERROR" -> minimumLevel = Level.ERROR.toInt();
                case "WARN" -> minimumLevel = Level.WARN.toInt();
                case "DEBUG" -> minimumLevel = Level.DEBUG.toInt();
                default -> minimumLevel = Level.INFO.toInt();
            }

            URI uri = URI.create(logURLString);
            IO.Options options = IO.Options.builder()
                    .setAuth(Collections.singletonMap("token", response))
                    .build();
            socket = IO.socket(uri, options);
            socket.connect();
            logBuilder = new Log.LogBuilder("narcotrack");
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
            logBuilder
                    .setLevel(event.getLevel().toString())
                    .setLogger(event.getLoggerName())
                    .setMessage(event.getFormattedMessage())
                    .setThread(event.getThreadName());
            if (event.getLevel().toInt() > Level.INFO.toInt() && event.getThrowableProxy() != null) {
                logBuilder.setThrowable(event.getThrowableProxy());
            }
            socket.emit("log", logBuilder.build().toJson());
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
