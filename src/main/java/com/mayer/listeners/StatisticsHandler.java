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
            LOGGER.info("Statistics of the last 30s: EEG Frames: {}, ({}/s), CurrentAssessment Frames: {}, ({}/s), ElectrodeCheck Frames: {}, PowerSpectrum Frames: {}", NarcotrackFrameType.EEG.getCount(), NarcotrackFrameType.EEG.getCount()/30, NarcotrackFrameType.CURRENT_ASSESSMENT.getCount(), NarcotrackFrameType.CURRENT_ASSESSMENT.getCount()/30, NarcotrackFrameType.ELECTRODE_CHECK.getCount(), NarcotrackFrameType.POWER_SPECTRUM.getCount());
            NarcotrackFrameType.resetCounters();
        }
    }
}
