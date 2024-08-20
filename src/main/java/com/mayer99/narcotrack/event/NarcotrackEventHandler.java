package com.mayer99.narcotrack.event;


import com.mayer99.narcotrack.event.events.*;

import java.time.Instant;

public interface NarcotrackEventHandler {

    default void onSystemStart() {

    }

    default void onDetachedElectrode() {

    }

    default void onLooseElectrode() {

    }

    default void onGoodElectrodes() {

    }

    default void onRecoverableError() {

    }

    default void onUnrecoverableError() {

    }

    default void onScheduleRestart() {

    }

    default void onRecordingStart(Instant time) {

    }

    default void onRecordingStop() {

    }

    default void onIntervalStart() {

    }

    default void onIntervalEnd() {

    }

    default void onReceivedData(byte[] data) {

    }

    default void onReceivedEEG(ReceivedEEGEvent event) {

    }

    default void onReceivedCurrentAssessment(ReceivedCurrentAssessmentEvent event) {

    }

    default void onReceivedPowerSpectrum(ReceivedPowerSpectrumEvent event) {

    }

    default void onReceivedElectrodeCheck(ReceivedElectrodeCheckEvent event) {

    }

    default void onHandleRemains(ReceivedRemainsEvent event) {

    }
}
