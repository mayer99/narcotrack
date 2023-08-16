package com.mayer.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class Log {

    private final String thread;
    private final String level;
    private final String message;
    private final String project;
    private final String logger;
    private Throwable throwable;
    public Log(ILoggingEvent event) {
        thread = event.getThreadName();
        level = event.getLevel().toString();
        message = event.getFormattedMessage();
        project = "narcotrack";
        logger = event.getLoggerName();
        if (event.getThrowableProxy() != null) {
            throwable = new Throwable(event.getThrowableProxy());
        }
    }
}
