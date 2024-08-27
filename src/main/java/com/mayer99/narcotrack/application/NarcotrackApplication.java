package com.mayer99.narcotrack.application;

import com.mayer99.logging.RemoteLoggingAppender;
import com.mayer99.narcotrack.event.NarcotrackEventManager;
import com.mayer99.narcotrack.event.handlers.*;
import com.mayer99.narcotrack.data.NarcotrackSerialPortReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class NarcotrackApplication {

    private final Logger LOGGER = LoggerFactory.getLogger(NarcotrackApplication.class);
    private final NarcotrackEventManager eventManager = new NarcotrackEventManager();
    private final NarcotrackSerialPortReader serialPortReader;

    public NarcotrackApplication() {
        LOGGER.info("NarcotrackApplication starting...");
        LOGGER.info("Application started at {}", Instant.now());
        checkLoggingAppender();
        eventManager.registerHandler(new HardwareModuleHandler(this));
        eventManager.registerHandler(new BackupFileHandler(this));
        eventManager.registerHandler(new BadElectrodeListener(this));
        eventManager.registerHandler(new DatabaseHandler(this));
        eventManager.registerHandler(new StatisticsHandler());
        eventManager.registerHandler(new RemoteEventHandler());

        serialPortReader = new NarcotrackSerialPortReader(this);
    }

    public static void main(String[] args) {
        new NarcotrackApplication();
    }

    public void scheduleRestart() {
        LOGGER.warn("################################################################");
        LOGGER.warn("Scheduling restart...");
        LOGGER.warn("################################################################");
        try {
            Runtime.getRuntime().exec("sudo shutdown -r now");
            LOGGER.info("Scheduled restart");
        } catch (Exception e) {
            LOGGER.error("Could not schedule restart", e);
        }
    }

    public void cleanupAndExit() {
        eventManager.cleanup();
        serialPortReader.cleanup();
        System.exit(0);
    }

    private void checkLoggingAppender() {
        if (RemoteLoggingAppender.disabled) {
            LOGGER.warn("RemoteLoggingAppender reports to be disabled");
        } else {
            LOGGER.info("RemoteLoggingAppender reports to be enabled");
        }
    }

    public static String getEnvironmentVariable(String key) throws IllegalArgumentException, NullPointerException {
        String value = System.getenv(key);
        if (isNullEmptyOrWhitespace(value)) throw new IllegalArgumentException(String.format("%s is null, empty or whitespace.", key));
        return value;
    }

    public static boolean isNullEmptyOrWhitespace(String value) {
        return value == null || value.trim().isEmpty();
    }

    public NarcotrackEventManager getEventManager() {
        return eventManager;
    }
}
