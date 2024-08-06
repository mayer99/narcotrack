package com.mayer99.narcotrack.base.models;


import com.mayer99.narcotrack.base.events.*;

import java.time.Instant;

public interface NarcotrackEventHandler {

    default void onEEG(EEGEvent event) {

    }

    default void onCurrentAssessment(CurrentAssessmentEvent event) {

    }

    default void onPowerSpectrum(PowerSpectrumEvent event) {

    }

    default void onElectrodeCheck(ElectrodeCheckEvent event) {

    }

    default void onRemains(RemainsEvent event) {

    }

    default void onEndOfInterval() {

    }

    default void onRecordingStart(Instant instant) {

    }

    default void onRecordingStop() {

    }

    default void onCriticalError() {

    }

    default void onBackup(byte[] data) {

    }

    default void onLooseElectrode() {

    }

    default void onDetachedElectrode() {

    }

    default void onGoodElectrodes() {

    }

}
