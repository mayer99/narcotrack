package com.mayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class NarcotrackStatistics {

    private static final Logger logger = LoggerFactory.getLogger(NarcotrackStatistics.class);
    private int recordId;
    private long startTimeLocal;
    private long startTimeNTP;
    private final DecimalFormat decimalFormat;
    private int intervallsWithoutData;
    private static int intervalDuration = 5;

    public NarcotrackStatistics(int recordId, long startTimeLocal, long startTimeNTP) {

        this.recordId = recordId;
        this.startTimeLocal = startTimeLocal;
        this.startTimeNTP = startTimeNTP;
        decimalFormat = new DecimalFormat("#.##");
        intervallsWithoutData = 0;
        ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
        ScheduledFuture<?> scheduledFuture = ses.scheduleAtFixedRate(() -> {
            if (Arrays.stream(NarcotrackPackageType.values()).allMatch(p -> p.getCountDifferential() == 0)) {
                intervallsWithoutData++;
                logger.debug("No data received during the last interval. Time without data is {}s", intervallsWithoutData * intervalDuration);
                if (intervallsWithoutData%(60/intervalDuration) == 0) {
                    logger.warn("No data received since {}min", (intervallsWithoutData * intervalDuration)/60);
                    if ((intervallsWithoutData * intervalDuration)/60 >= 2) {
                        logger.error("Did not receive data for more than 2 minutes. Restarting...");
                        System.exit(1);
                    }
                }
                return;
            }
            intervallsWithoutData = 0;
            logger.info("Statistik der letzten {} Sekunden: \n{} EEGs/s \n{} CurrentAssessments/s \n{} Power Spectrum \n{} Electrode Check",
                    intervalDuration,
                    Math.round(NarcotrackPackageType.EEG.getCountDifferential()/intervalDuration * 100) / 100,
                    Math.round(NarcotrackPackageType.CURRENT_ASSESSMENT.getCountDifferential()/intervalDuration * 100) / 100,
                    NarcotrackPackageType.POWER_SPECTRUM.getCountDifferential(),
                    NarcotrackPackageType.ELECTRODE_CHECK.getCountDifferential());
            for (NarcotrackPackageType p: NarcotrackPackageType.values()) {
                p.resetCounter();
            }
        }, intervalDuration, intervalDuration, TimeUnit.SECONDS);
    }

}
