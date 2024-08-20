package com.mayer99.narcotrack.application;

import com.mayer99.narcotrack.events.NarcotrackEventManager;
import com.mayer99.narcotrack.events.handlers.*;
import com.mayer99.narcotrack.serial.NarcotrackSerialPortReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Properties;

public class NarcotrackApplication {

    private final Logger LOGGER = LoggerFactory.getLogger(NarcotrackApplication.class);
    private final Instant APPLICATION_START_TIME = Instant.now();
    private final NarcotrackEventManager eventManager = new NarcotrackEventManager();
    private final Properties properties = new Properties();

    public NarcotrackApplication() {
        LOGGER.info("NarcotrackApplication starting...");
        LOGGER.info("Application started at {}", APPLICATION_START_TIME);
        Runtime.getRuntime().addShutdownHook(new NarcotrackApplicationShutdownHook());
        loadConfigFile();
        eventManager.registerHandler(new HardwareModuleHandler(this));
        eventManager.registerHandler(new BackupFileHandler());
        eventManager.registerHandler(new BadElectrodeListener(this));
        eventManager.registerHandler(new DatabaseHandler(this));
        eventManager.registerHandler(new StatisticsHandler());

        new NarcotrackSerialPortReader(this);
    }

    class NarcotrackApplicationShutdownHook extends Thread {
        @Override
        public void run() {
            LOGGER.warn("Exiting NarcotrackApplication...");
        }
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
        } catch (IOException e) {
            LOGGER.error("Could not schedule a restart", e);
        }
    }

    private void loadConfigFile() {
        LOGGER.info("Loading config file...");
        try (InputStream input = new FileInputStream("narcotrack.config")) {
            properties.load(input);
        } catch (SecurityException | IOException | IllegalArgumentException | NullPointerException e) {
            LOGGER.error("Could not load config file", e);
            eventManager.dispatchOnUnrecoverableError();
            System.exit(1);
        }
    }

    public Properties getProperties() {
        return properties;
    }

    public String getConfig(String key) {
        return properties.getProperty(key);
    }

    public NarcotrackEventManager getEventManager() {
        return eventManager;
    }
}
