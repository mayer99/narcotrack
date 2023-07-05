package com.mayer.listeners;

import com.mayer.NarcotrackEventHandler;
import com.mayer.events.CurrentAssessmentEvent;
import com.mayer.events.EEGEvent;
import com.mayer.events.ElectrodeCheckEvent;
import com.mayer.events.PowerSpectrumEvent;
import com.mayer.events.RemainsEvent;
import com.mayer.NarcotrackFrameType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class StatisticHandler implements NarcotrackEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatisticHandler.class);

    private final int INTERVAL_DURATION = 30;

    public StatisticHandler() {
        LOGGER.info("starting...");

        ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
        ScheduledFuture<?> scheduledFuture = ses.scheduleAtFixedRate(() -> {
            LOGGER.info("Statistics for the last {}s", INTERVAL_DURATION);
            LOGGER.info("EEGs: {} ({}/s)", NarcotrackFrameType.EEG.getCount(), (NarcotrackFrameType.EEG.getCount() / INTERVAL_DURATION));
            LOGGER.info("CurrentAssessments: {} ({}/s)", NarcotrackFrameType.CURRENT_ASSESSMENT.getCount(), (NarcotrackFrameType.CURRENT_ASSESSMENT.getCount() / INTERVAL_DURATION));
            LOGGER.info("PowerSpectrums: {}", NarcotrackFrameType.POWER_SPECTRUM.getCount());
            LOGGER.info("ElectrodeChecks: {}", NarcotrackFrameType.ELECTRODE_CHECK.getCount());
            for (NarcotrackFrameType frame: NarcotrackFrameType.values()) {
                frame.resetCounter();
            }
        }, INTERVAL_DURATION, INTERVAL_DURATION, TimeUnit.SECONDS);
        EEGEvent.getEventHandlers().add(this);
        CurrentAssessmentEvent.getEventHandlers().add(this);
        PowerSpectrumEvent.getEventHandlers().add(this);
        ElectrodeCheckEvent.getEventHandlers().add(this);
        RemainsEvent.getEventHandlers().add(this);
    }

    @Override
    public void onEEGEvent(EEGEvent event) {
        NarcotrackFrameType.EEG.count();
    }

    @Override
    public void onCurrentAssessmentEvent(CurrentAssessmentEvent event) {
        NarcotrackFrameType.CURRENT_ASSESSMENT.count();
    }

    @Override
    public void onPowerSpectrumEvent(PowerSpectrumEvent event) {
        NarcotrackFrameType.POWER_SPECTRUM.count();
    }


    @Override
    public void onElectrodeCheckEvent(ElectrodeCheckEvent event) {
        NarcotrackFrameType.ELECTRODE_CHECK.count();
    }

    @Override
    public void onRemainsEvent(RemainsEvent event) {
        LOGGER.warn("Remains with {} chunks, time: {}", event.getData().getChunks().size(), event.getTime());
    }
}
