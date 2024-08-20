package com.mayer99.narcotrack.events.handlers;

import com.mayer99.narcotrack.events.NarcotrackFrameType;
import com.mayer99.narcotrack.events.NarcotrackEventHandler;
import com.mayer99.narcotrack.events.ReceivedCurrentAssessmentEvent;
import com.mayer99.narcotrack.events.ReceivedEEGEvent;
import com.mayer99.narcotrack.events.ReceivedElectrodeCheckEvent;
import com.mayer99.narcotrack.events.ReceivedPowerSpectrumEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;

public class StatisticsHandler implements NarcotrackEventHandler {

    private final Logger LOGGER = LoggerFactory.getLogger(StatisticsHandler.class);
    private int counter = 0;
    private final HashMap<NarcotrackFrameType, Integer> frameCounts = new HashMap<>();

    public StatisticsHandler() {
        resetFrameCounts();
    }

    private void resetFrameCounts() {
        for (NarcotrackFrameType frameType : NarcotrackFrameType.values()) {
            frameCounts.put(frameType, 0);
        }
    }

    @Override
    public void onRecordingStart(Instant time) {
        counter = 0;
        resetFrameCounts();
    }

    @Override
    public void onIntervalEnd() {
        counter++;
        if (counter >= 30) {
            counter = 0;
            LOGGER.info("Statistics of the last 30s:");
            for (NarcotrackFrameType frameType : NarcotrackFrameType.values()) {
                LOGGER.info("Received {} frames of type {}", frameCounts.get(frameType), frameType);
            }
            if ((float) frameCounts.get(NarcotrackFrameType.EEG)/30 <= 15) {
                LOGGER.warn("Did not receive enough frames of type EEG. On average {}/s", (float) frameCounts.get(NarcotrackFrameType.EEG)/30);
            }
            if ((float) frameCounts.get(NarcotrackFrameType.CURRENT_ASSESSMENT)/30 <= 0.95) {
                LOGGER.warn("Did not receive enough frames of type CURRENT_ASSESSMENT. On average {}/s", (float) frameCounts.get(NarcotrackFrameType.CURRENT_ASSESSMENT)/30);
            }
        }
    }

    @Override
    public void onReceivedEEG(ReceivedEEGEvent event) {
        frameCounts.put(NarcotrackFrameType.EEG, frameCounts.get(NarcotrackFrameType.EEG) + 1);
    }

    @Override
    public void onReceivedCurrentAssessment(ReceivedCurrentAssessmentEvent event) {
        frameCounts.put(NarcotrackFrameType.CURRENT_ASSESSMENT, frameCounts.get(NarcotrackFrameType.CURRENT_ASSESSMENT) + 1);
    }

    @Override
    public void onReceivedPowerSpectrum(ReceivedPowerSpectrumEvent event) {
        frameCounts.put(NarcotrackFrameType.POWER_SPECTRUM, frameCounts.get(NarcotrackFrameType.POWER_SPECTRUM) + 1);
    }

    @Override
    public void onReceivedElectrodeCheck(ReceivedElectrodeCheckEvent event) {
        frameCounts.put(NarcotrackFrameType.ELECTRODE_CHECK, frameCounts.get(NarcotrackFrameType.ELECTRODE_CHECK) + 1);
    }
}
