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
        String authURLString = System.getenv("NARCOTRACK_AUTH_URL");
        String logURLString = System.getenv("NARCOTRACK_LOG_URL");
        String logLevel = System.getenv("NARCOTRACK_LOGLEVEL");
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
            logLevel = "";
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
            System.out.println("Before jwt print");
            addInfo("Received JWT: " + jwt);
            System.out.println("After jwt print");

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
            System.out.println(e.getMessage());
            addError("Could not initialize socket appender", e);
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
            JSONObject logData = new JSONObject();
            logData
                    .put("thread", event.getThreadName())
                    .put("level", event.getLevel().toString())
                    .put("message", event.getFormattedMessage())
                    .put("project", "narcotrack")
                    .put("logger", event.getLoggerName());
            if (event.getLevel().toInt() > Level.INFO.toInt() && event.getThrowableProxy() != null) {
                IThrowableProxy throwable = event.getThrowableProxy();
                JSONObject exceptionData = new JSONObject();
                exceptionData.put("message", throwable.getMessage() != null & !throwable.getMessage().trim().isEmpty() ? throwable.getMessage() : "No message");
                if (throwable.getStackTraceElementProxyArray() != null) {
                    exceptionData.put("trace",
                            Arrays.stream(throwable.getStackTraceElementProxyArray())
                            .filter(stackTraceElementProxy -> stackTraceElementProxy.getStackTraceElement() != null)
                                    .limit(10)
                            .map(stackTraceElementProxy -> stackTraceElementProxy.getStackTraceElement().toString())
                                    .collect(Collectors.joining("<br>"))
                    );
                }
                logData.put("exception", exceptionData);
            }
            System.out.println(logData.toString());
            socket.emit("log", logData);
        } catch (Exception e) {
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
