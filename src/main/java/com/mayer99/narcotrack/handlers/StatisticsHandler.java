package com.mayer99.narcotrack.handlers;

import com.mayer99.narcotrack.base.models.NarcotrackEventHandler;
import com.mayer99.narcotrack.base.models.NarcotrackFrameType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatisticsHandler implements NarcotrackEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsHandler.class);
    private int counter = 0;

    public StatisticsHandler() {
    }

    @Override
    public void onEndOfInterval() {
        counter++;
        if (counter >= 30) {
            counter = 0;
            LOGGER.info("Statistics of the last 30s: Received {} EEG Frames, {} CURRENT_ASSESSMENT Frames, {} POWER_SPECTRUM FRAMES AND {} ELECTRODE_CHECK Frames", NarcotrackFrameType.EEG.getCount(), NarcotrackFrameType.CURRENT_ASSESSMENT.getCount(), NarcotrackFrameType.POWER_SPECTRUM.getCount(), NarcotrackFrameType.ELECTRODE_CHECK.getCount());
            if ((float) NarcotrackFrameType.EEG.getCount()/30 <= 15) {
                LOGGER.warn("Only received {} EEG Frames in the last 30s ({}/s)", NarcotrackFrameType.EEG.getCount(), (float) NarcotrackFrameType.EEG.getCount()/30);
            }
            if ((float) NarcotrackFrameType.CURRENT_ASSESSMENT.getCount()/30 <= 0.95) {
                LOGGER.warn("Only received {} CURRENT_ASSESSMENT Frames in the last 30s ({}/s)", NarcotrackFrameType.CURRENT_ASSESSMENT.getCount(), (float) NarcotrackFrameType.CURRENT_ASSESSMENT.getCount()/30);
            }
            NarcotrackFrameType.resetCounters();
        }
    }
}
