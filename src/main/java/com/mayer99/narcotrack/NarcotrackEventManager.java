package com.mayer99.narcotrack;

import com.mayer99.narcotrack.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class NarcotrackEventManager {

    private final Logger LOGGER = LoggerFactory.getLogger(NarcotrackEventManager.class);

    private final List<NarcotrackEventHandler> handlers = new ArrayList<>();

    public void registerHandler(NarcotrackEventHandler handler) {
        handlers.add(handler);
    }

    public void dispatchOnSystemStart() {
        LOGGER.debug("Dispatching onSystemStart");
        handlers.forEach(NarcotrackEventHandler::onSystemStart);
    }

    public void dispatchOnDetachedElectrode() {
        LOGGER.debug("Dispatching onDetachedElectrode");
        handlers.forEach(NarcotrackEventHandler::onDetachedElectrode);
    }

    public void dispatchOnLooseElectrode() {
        LOGGER.debug("Dispatching onLooseElectrode");
        handlers.forEach(NarcotrackEventHandler::onLooseElectrode);
    }

    public void dispatchOnGoodElectrodes() {
        LOGGER.debug("Dispatching onGoodElectrodes");
        handlers.forEach(NarcotrackEventHandler::onGoodElectrodes);
    }

    public void dispatchOnRecoverableError() {
        LOGGER.debug("Dispatching onRecoverableError");
        handlers.forEach(NarcotrackEventHandler::onRecoverableError);
    }

    public void dispatchOnUnrecoverableError() {
        LOGGER.debug("Dispatching onUnrecoverableError");
        handlers.forEach(NarcotrackEventHandler::onUnrecoverableError);
    }

    public void dispatchOnScheduleRestart() {
        LOGGER.debug("Dispatching onScheduleRestart");
        handlers.forEach(NarcotrackEventHandler::onScheduleRestart);
    }

    public void dispatchOnRecordingStart(Instant time) {
        LOGGER.debug("Dispatching onRecordingStart");
        handlers.forEach(handler -> handler.onRecordingStart(time));
    }

    public void dispatchOnRecordingStop() {
        LOGGER.debug("Dispatching onRecordingStop");
        handlers.forEach(NarcotrackEventHandler::onRecordingStop);
    }

    public void dispatchOnIntervalStart() {
        LOGGER.debug("Dispatching onIntervalStart");
        handlers.forEach(NarcotrackEventHandler::onIntervalStart);
    }

    public void dispatchOnIntervalStop() {
        LOGGER.debug("Dispatching onIntervalStop");
        handlers.forEach(NarcotrackEventHandler::onIntervalStop);
    }

    public void dispatchOnReceivedData(byte[] data) {
        LOGGER.debug("Dispatching onReceivedData");
        handlers.forEach(handler -> handler.onReceivedData(data));
    }

    public void dispatchOnReceivedEEG(ReceivedEEGEvent event) {
        LOGGER.debug("Dispatching onReceivedEEG");
        handlers.forEach(handler -> handler.onReceivedEEG(event));
    }

    public void dispatchOnReceivedCurrentAssessment(ReceivedCurrentAssessmentEvent event) {
        LOGGER.debug("Dispatching onDataAvailable");
        handlers.forEach(handler -> handler.onReceivedCurrentAssessment(event));
    }

    public void dispatchOnReceivedPowerSpectrum(ReceivedPowerSpectrumEvent event) {
        LOGGER.debug("Dispatching onReceivedPowerSpectrum");
        handlers.forEach(handler -> handler.onReceivedPowerSpectrum(event));
    }

    public void dispatchOnReceivedElectrodeCheck(ReceivedElectrodeCheckEvent event) {
        LOGGER.debug("Dispatching onReceivedElectrodeCheck");
        handlers.forEach(handler -> handler.onReceivedElectrodeCheck(event));
    }

    public void dispatchOnHandleRemains(HandleRemainsEvent event) {
        LOGGER.debug("Dispatching onHandleRemains");
        handlers.forEach(handler -> handler.onHandleRemains(event));
    }
}