package com.mayer99.narcotrack.event.handlers;

import com.mayer99.narcotrack.data.NarcotrackFrame;
import com.mayer99.narcotrack.event.NarcotrackEventHandler;
import com.mayer99.narcotrack.event.events.ReceivedCurrentAssessmentEvent;
import com.mayer99.narcotrack.event.events.ReceivedEEGEvent;
import com.mayer99.narcotrack.event.events.ReceivedElectrodeCheckEvent;
import com.mayer99.narcotrack.event.events.ReceivedPowerSpectrumEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;

public class StatisticsHandler implements NarcotrackEventHandler {

    private final Logger LOGGER = LoggerFactory.getLogger(StatisticsHandler.class);
    private int counter = 0;
    private final HashMap<NarcotrackFrame, Integer> frameCounts = new HashMap<>();

    public StatisticsHandler() {
        resetFrameCounts();
    }

    private void resetFrameCounts() {
        for (NarcotrackFrame frameType : NarcotrackFrame.values()) {
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
            for (NarcotrackFrame frameType : NarcotrackFrame.values()) {
                LOGGER.info("Received {} frames of type {}", frameCounts.get(frameType), frameType);
            }
            if ((float) frameCounts.get(NarcotrackFrame.EEG)/30 <= 15) {
                LOGGER.warn("Did not receive enough frames of type EEG. On average {}/s", (float) frameCounts.get(NarcotrackFrame.EEG)/30);
            }
            if ((float) frameCounts.get(NarcotrackFrame.CURRENT_ASSESSMENT)/30 <= 0.95) {
                LOGGER.warn("Did not receive enough frames of type CURRENT_ASSESSMENT. On average {}/s", (float) frameCounts.get(NarcotrackFrame.CURRENT_ASSESSMENT)/30);
            }
        }
    }

    @Override
    public void onReceivedEEG(ReceivedEEGEvent event) {
        frameCounts.put(NarcotrackFrame.EEG, frameCounts.get(NarcotrackFrame.EEG) + 1);
    }

    @Override
    public void onReceivedCurrentAssessment(ReceivedCurrentAssessmentEvent event) {
        frameCounts.put(NarcotrackFrame.CURRENT_ASSESSMENT, frameCounts.get(NarcotrackFrame.CURRENT_ASSESSMENT) + 1);
    }

    @Override
    public void onReceivedPowerSpectrum(ReceivedPowerSpectrumEvent event) {
        frameCounts.put(NarcotrackFrame.POWER_SPECTRUM, frameCounts.get(NarcotrackFrame.POWER_SPECTRUM) + 1);
    }

    @Override
    public void onReceivedElectrodeCheck(ReceivedElectrodeCheckEvent event) {
        frameCounts.put(NarcotrackFrame.ELECTRODE_CHECK, frameCounts.get(NarcotrackFrame.ELECTRODE_CHECK) + 1);
    }
}
