package com.mayer.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import io.socket.client.IO;
import io.socket.client.Socket;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.Collections;

public class SocketAppender extends AppenderBase<ILoggingEvent> {

    private static final String NARCOTRACK_API_URL = System.getenv("NARCOTRACK_API_URL");
    private static final String NARCOTRACK_API_TOKEN = System.getenv("NARCOTRACK_API_TOKEN");
    private String prefix;
    private boolean guard = false;
    private URI uri;
    private IO.Options options;
    private Socket socket;

    public SocketAppender() {
        uri = URI.create(NARCOTRACK_API_URL);
        options = IO.Options.builder()
                .setAuth(Collections.singletonMap("token", NARCOTRACK_API_TOKEN))
                .build();
        socket = IO.socket(uri, options);
        socket.connect();
    }

    @Override
    protected void append(ILoggingEvent eventObject) {

        if (guard == true) {
            return;
        }
        guard = true;
        try {
            JSONObject eventData = new JSONObject();
            eventData.put("level", eventObject.getLevel().toString());
            eventData.put("message", eventObject.getFormattedMessage());
            eventData.put("timestamp", eventObject.getTimeStamp());
            eventData.put("logger", eventObject.getLoggerName());
            socket.emit("log", eventData);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        guard = false;
    }


    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
