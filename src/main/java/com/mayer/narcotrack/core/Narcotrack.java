package com.mayer.narcotrack.core;

import com.fazecast.jSerialComm.SerialPort;
import com.mayer.narcotrack.core.events.*;
import com.mayer.narcotrack.core.handler.BackupFileHandler;
import com.mayer.narcotrack.core.handler.SerialPortHandler;
import com.mayer.narcotrack.core.models.NarcotrackFrameType;
import com.mayer.lights.StatusLights;
import com.mayer.narcotrack.core.models.NarcotrackEventHandler;
import com.mayer.narcotrack.handler.ElectrodeDisconnectedListener;
import com.mayer.narcotrack.handler.MariaDatabaseHandler;
import com.mayer.narcotrack.handler.StatisticsHandler;
import com.mayer.narcotrack.handler.StatusLightsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Narcotrack {

    private static final Logger LOGGER = LoggerFactory.getLogger(Narcotrack.class);
    private static final ArrayList<NarcotrackEventHandler> HANDLERS = new ArrayList<>();

    private final Instant startTime;
    private final StatusLights statusLights;

    private Narcotrack() {
        LOGGER.info("Application starting...");

        startTime = Instant.now();
        statusLights = new StatusLights();
        SerialPortHandler serialPortHandler = new SerialPortHandler(this);

        HANDLERS.add(new MariaDatabaseHandler(this));
        HANDLERS.add(new ElectrodeDisconnectedListener(this));
        HANDLERS.add(new StatisticsHandler());
        HANDLERS.add(new StatusLightsHandler(this));
    }

    public static void rebootPlatform() {
        LOGGER.error("Preparing reboot...");
        if (System.getenv("PLEASE_DO_NOT_RESTART") != null) {
            LOGGER.info("Restart aborted, anti restart flag has been set");
            return;
        }
        try {
            Runtime.getRuntime().exec("sudo shutdown -r now");
        } catch (IOException e) {
            LOGGER.error("Cannot restart system", e);
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        new Narcotrack();
    }

    public Instant getStartTime() {
        return startTime;
    }

    public StatusLights getStatusLights() {
        return statusLights;
    }

    public static ArrayList<NarcotrackEventHandler> getHandlers() {
        return HANDLERS;
    }
}