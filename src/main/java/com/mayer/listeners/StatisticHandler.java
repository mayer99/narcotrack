package com.mayer.listeners;

import com.mayer.events.NarcotrackEventHandler;
import com.mayer.events.CurrentAssessmentEvent;
import com.mayer.events.EEGEvent;
import com.mayer.events.ElectrodeCheckEvent;
import com.mayer.events.PowerSpectrumEvent;
import com.mayer.events.RemainsEvent;
import com.mayer.NarcotrackFrames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class StatisticHandler implements NarcotrackEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatisticHandler.class);

    private final int intervalDuration;

    public StatisticHandler() {

        LOGGER.info("starting...");

        intervalDuration = 30;

        ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
        ScheduledFuture<?> scheduledFuture = ses.scheduleAtFixedRate(() -> {
            LOGGER.info("Statistics for the last {}s:\nEEGs: {} ({}/s)\nCurrentAssessments: {} ({}/s)\nPowerSpectrums: {}\nElectrodeChecks: {}", intervalDuration, NarcotrackFrames.EEG.getCount(), (NarcotrackFrames.EEG.getCount() / intervalDuration), NarcotrackFrames.CURRENT_ASSESSMENT.getCount(), (NarcotrackFrames.CURRENT_ASSESSMENT.getCount() / intervalDuration), NarcotrackFrames.POWER_SPECTRUM.getCount(), NarcotrackFrames.ELECTRODE_CHECK.getCount());
            for (NarcotrackFrames frame: NarcotrackFrames.values()) {
                frame.resetCounter();
            }
        }, intervalDuration, intervalDuration, TimeUnit.SECONDS);
        EEGEvent.getEventHandlers().add(this);
        CurrentAssessmentEvent.getEventHandlers().add(this);
        PowerSpectrumEvent.getEventHandlers().add(this);
        ElectrodeCheckEvent.getEventHandlers().add(this);
        RemainsEvent.getEventHandlers().add(this);
    }

    @Override
    public void onEEGEvent(EEGEvent event) {
        NarcotrackFrames.EEG.count();
    }

    @Override
    public void onCurrentAssessmentEvent(CurrentAssessmentEvent event) {
        NarcotrackFrames.CURRENT_ASSESSMENT.count();
    }

    @Override
    public void onPowerSpectrumEvent(PowerSpectrumEvent event) {
        NarcotrackFrames.POWER_SPECTRUM.count();
    }


    @Override
    public void onElectrodeCheckEvent(ElectrodeCheckEvent event) {
        NarcotrackFrames.ELECTRODE_CHECK.count();
    }

    @Override
    public void onRemainsEvent(RemainsEvent event) {
        LOGGER.warn("Received remains of length {}", event.getData().getChunks().size());
    }
}
