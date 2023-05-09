package com.mayer.listeners;

import com.mayer.events.*;
import com.mayer.factory.NarcotrackEventHandler;
import com.mayer.frames.NarcotrackFrames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class StatisticHandler implements NarcotrackEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatisticHandler.class);

    private final long startTimeLocal;
    private final long startTimeNTP;
    private int intervallsWithoutData;
    private final int intervalDuration;
    private final HashMap<NarcotrackFrames, Integer> differentialFrameCount;

    public StatisticHandler(long startTimeLocal, long startTimeNTP) {

        this.startTimeLocal = startTimeLocal;
        this.startTimeNTP = startTimeNTP;
        intervallsWithoutData = 0;
        intervalDuration = 5;
        differentialFrameCount = new HashMap<>() {{
            put(NarcotrackFrames.EEG, 0);
            put(NarcotrackFrames.CURRENT_ASSESSMENT, 0);
            put(NarcotrackFrames.POWER_SPECTRUM, 0);
            put(NarcotrackFrames.ELECTRODE_CHECK, 0);
        }};

        ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
        ScheduledFuture<?> scheduledFuture = ses.scheduleAtFixedRate(() -> {
            if (!receivedDataSinceLastInterval()) {
                intervallsWithoutData++;
                LOGGER.debug("No data received for {}s", intervalDuration * intervallsWithoutData);
                if (intervallsWithoutData%(60/intervalDuration) != 0) return;
                LOGGER.warn("No data received for {}mins", (intervallsWithoutData * intervalDuration)/60);
                if ((intervallsWithoutData * intervalDuration)/60 >= 2) {
                    LOGGER.error("No data received for 2 minutes");
                    // Hier könnte man Spaß haben mit System restarts
                    if (System.getenv("PLEASE_DO_NOT_RESTART") == null) {
                        try {
                            LOGGER.info("Restarting...");
                            Runtime.getRuntime().exec("sudo shutdown -r now");
                        } catch (IOException e) {
                            LOGGER.error("Cannot restart system");
                            System.exit(1);
                        }
                    } else {
                        LOGGER.info("Did not restart because of PLEASE_DO_NOT_RESTART");
                    }
                }
            }
        }, intervalDuration, intervalDuration, TimeUnit.SECONDS);

    }

    @Override
    public void onEEGEvent(EEGEvent event) {
        differentialFrameCount.merge(NarcotrackFrames.EEG, 1, Integer::sum);
    }

    @Override
    public void onCurrentAssessmentEvent(CurrentAssessmentEvent event) {
        differentialFrameCount.merge(NarcotrackFrames.CURRENT_ASSESSMENT, 1, Integer::sum);
    }

    @Override
    public void onPowerSpectrumEvent(PowerSpectrumEvent event) {
        differentialFrameCount.merge(NarcotrackFrames.POWER_SPECTRUM, 1, Integer::sum);
    }


    @Override
    public void onElectrodeCheckEvent(ElectrodeCheckEvent event) {
        differentialFrameCount.merge(NarcotrackFrames.ELECTRODE_CHECK, 1, Integer::sum);
    }

    @Override
    public void onRemainsEvent(RemainsEvent event) {
        LOGGER.warn("Received remains of length {}", event.getData().getChunks().size());
    }

    private boolean receivedDataSinceLastInterval() {
        return differentialFrameCount.values().stream().allMatch(value -> value == 0);
    }
}
