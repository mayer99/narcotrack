package com.mayer99;

import com.mayer99.lights.StatusLights;
import com.mayer99.narcotrack.base.models.NarcotrackEventHandler;
import com.mayer99.narcotrack.base.handler.SerialPortHandler;
import com.mayer99.narcotrack.handlers.ElectrodeDisconnectedListener;
import com.mayer99.narcotrack.handlers.MariaDatabaseHandler;
import com.mayer99.narcotrack.handlers.StatisticsHandler;
import com.mayer99.narcotrack.handlers.StatusLightsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;

public class Narcotrack {

    private static final Logger LOGGER = LoggerFactory.getLogger(Narcotrack.class);
    private static final ArrayList<NarcotrackEventHandler> HANDLERS = new ArrayList<>();

    private final Instant startTime;
    private final StatusLights statusLights;

    private Narcotrack() {
        LOGGER.info("Application starting...");

        startTime = Instant.now();
        LOGGER.info("StartTime: {}", startTime.toString());

        statusLights = new StatusLights();
        new SerialPortHandler(this);

        HANDLERS.add(new MariaDatabaseHandler(this));
        HANDLERS.add(new ElectrodeDisconnectedListener(this));
        HANDLERS.add(new StatisticsHandler());
        HANDLERS.add(new StatusLightsHandler(this));
    }

    public static void rebootPlatform() {
        LOGGER.error("Reboot ################################################################################");
        if (System.getenv("PLEASE_DO_NOT_RESTART") == null) {
            try {
                Runtime.getRuntime().exec("sudo shutdown -r now");
            } catch (IOException e) {
                LOGGER.error("Cannot restart system", e);
            }
        } else {
            LOGGER.info("Did not restart because of PLEASE_DO_NOT_RESTART");
        }
        System.exit(1);
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