package com.mayer;

import com.mayer.events.*;

public interface NarcotrackEventHandler {

    default void onEEGEvent(EEGEvent event) {

    }

    default void onCurrentAssessmentEvent(CurrentAssessmentEvent event) {

    }

    default void onPowerSpectrumEvent(PowerSpectrumEvent event) {

    }

    default void onElectrodeCheckEvent(ElectrodeCheckEvent event) {

    }

    default void onRemainsEvent(RemainsEvent event) {

    }

}
