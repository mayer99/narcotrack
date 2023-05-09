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

    private final String narcotrackApiUrl = System.getenv("NARCOTRACK_API_URL");
    private final String narcotrackApiToken = System.getenv("NARCOTRACK_API_TOKEN");
    private String prefix;
    private boolean guard = false;
    private URI uri;
    private IO.Options options;
    private Socket socket;

    public SocketAppender() {

        try {
            System.out.println("URL " + narcotrackApiUrl);
            uri = URI.create(narcotrackApiUrl);
            options = IO.Options.builder()
                    .setAuth(Collections.singletonMap("token", narcotrackApiToken))
                    .build();
            socket = IO.socket(uri, options);
            socket.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
