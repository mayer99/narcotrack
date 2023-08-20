package com.mayer.listeners;

import com.mayer.Narcotrack;
import com.mayer.NarcotrackEventHandler;
import com.mayer.NarcotrackFrameType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatisticsHandler implements NarcotrackEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsHandler.class);

    public StatisticsHandler() {
        Narcotrack.registerNarcotrackEventListener(this);
    }
    private int counter = 0;
    @Override
    public void onEndOfInterval() {
        counter++;
        if (counter >= 30) {
            counter = 0;
            LOGGER.info("Statistics of the last 30s:");
            LOGGER.info("EEG Frames: {}, ({}/s)", NarcotrackFrameType.EEG.getCount(), NarcotrackFrameType.EEG.getCount()/30);
            LOGGER.info("CurrentAssessment Frames: {}, ({}/s)", NarcotrackFrameType.CURRENT_ASSESSMENT.getCount(), NarcotrackFrameType.CURRENT_ASSESSMENT.getCount()/30);
            LOGGER.info("ElectrodeCheck Frames: {}", NarcotrackFrameType.ELECTRODE_CHECK.getCount());
            LOGGER.info("PowerSpectrum Frames: {}", NarcotrackFrameType.POWER_SPECTRUM.getCount());
            NarcotrackFrameType.resetCounters();
        }
    }
}
