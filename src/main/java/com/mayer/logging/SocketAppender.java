package com.mayer.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import io.socket.client.IO;
import io.socket.client.Socket;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.Collections;

public class SocketAppender extends AppenderBase<ILoggingEvent> {

    private int minimumLevel;
    private boolean active;
    private boolean guard;
    private Socket socket;

    public SocketAppender() {

        String narcotrackApiUrl = System.getenv("NARCOTRACK_API_URL");
        String narcotrackApiToken = System.getenv("NARCOTRACK_API_TOKEN");
        if (isNullEmptyOrWhitespace(narcotrackApiUrl) || isNullEmptyOrWhitespace(narcotrackApiToken)) {
            if (isNullEmptyOrWhitespace(narcotrackApiUrl)) addError("Could not start SocketAppender, because narcotrackApiUrl is null, empty or whitespace");
            if (isNullEmptyOrWhitespace(narcotrackApiToken)) addError("Could not start SocketAppender, because narcotrackApiToken is null, empty or whitespace");
            active = false;
            return;
        }

        try {
            String narcotrackApiLevel = !isNullEmptyOrWhitespace(System.getenv(("NARCOTRACK_API_LOGLEVEL"))) ? System.getenv(("NARCOTRACK_API_LOGLEVEL")) : "INFO";
            switch(narcotrackApiLevel) {
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

            URI uri = URI.create(narcotrackApiUrl);
            IO.Options options = IO.Options.builder()
                    .setAuth(Collections.singletonMap("token", narcotrackApiToken))
                    .build();
            socket = IO.socket(uri, options);
            socket.connect();
            active = true;
        } catch (Exception e) {
            active = false;
            addError("Could not start SocketAppender", e);
        }
    }

    @Override
    protected void append(ILoggingEvent eventObject) {

        if (minimumLevel > eventObject.getLevel().toInt()) {
            return;
        }

        if (guard || !active) {
            return;
        }

        guard = true;
        JSONObject eventData = new JSONObject();
        try {
            eventData
                    .put("thread", eventObject.getThreadName())
                    .put("level", eventObject.getLevel().toString())
                    .put("message", eventObject.getFormattedMessage())
                    .put("timestamp", eventObject.getTimeStamp())
                    .put("logger", eventObject.getLoggerName());
            socket.emit("log", eventData);
        } catch (JSONException e) {
            addWarn("Unable to send log event", e);
        }

        guard = false;
    }

    private boolean isNullEmptyOrWhitespace(String data) {
        return data == null || data.trim().isEmpty();
    }

}
