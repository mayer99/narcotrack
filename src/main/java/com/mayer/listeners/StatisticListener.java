package com.mayer.listeners;

import com.mayer.factory.NarcotrackFrameListener;
import com.mayer.frames.NarcotrackFrames;
import com.mayer.Remains;
import com.mayer.frames.CurrentAssessment;
import com.mayer.frames.EEG;
import com.mayer.frames.ElectrodeCheck;
import com.mayer.frames.PowerSpectrum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class StatisticListener implements NarcotrackFrameListener {

    private static final Logger logger = LoggerFactory.getLogger(StatisticListener.class);
    private final long startTimeLocal;
    private final long startTimeNTP;
    private int intervallsWithoutData;
    private final int intervalDuration;
    private final HashMap<NarcotrackFrames, Integer> differentialFrameCount;

    public StatisticListener(long startTimeLocal, long startTimeNTP) {

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
                logger.debug("No data received for {}s", intervalDuration * intervallsWithoutData);
                if (intervallsWithoutData%(60/intervalDuration) != 0) return;
                logger.warn("No data received for {}mins", (intervallsWithoutData * intervalDuration)/60);
                if ((intervallsWithoutData * intervalDuration)/60 >= 2) {
                    logger.error("No data received for 2 minutes, restarting...");
                    // Hier könnte man Spaß haben mit System restarts
                    if (System.getenv("PLEASE_DO_NOT_RESTART") == null) {
                        try {
                            Runtime.getRuntime().exec("shutudown -r now");
                        } catch (IOException e) {
                            logger.error("Cannot restart system");
                            System.exit(1);
                        }
                    }
                }
            }
        }, intervalDuration, intervalDuration, TimeUnit.SECONDS);

    }

    @Override
    public void onEEG(EEG data) {
        differentialFrameCount.merge(NarcotrackFrames.EEG, 1, Integer::sum);
    }

    @Override
    public void onCurrentAssessment(CurrentAssessment data) {
        differentialFrameCount.merge(NarcotrackFrames.CURRENT_ASSESSMENT, 1, Integer::sum);
    }

    @Override
    public void onPowerSpectrum(PowerSpectrum data) {
        differentialFrameCount.merge(NarcotrackFrames.POWER_SPECTRUM, 1, Integer::sum);
    }


    @Override
    public void onElectrodeCheck(ElectrodeCheck data) {
        differentialFrameCount.merge(NarcotrackFrames.ELECTRODE_CHECK, 1, Integer::sum);
    }

    @Override
    public void onRemains(Remains data) {
        logger.warn("Received remains of length {}", data.getData().size());
    }

    private boolean receivedDataSinceLastInterval() {
        return differentialFrameCount.values().stream().allMatch(value -> value == 0);
    }
}
