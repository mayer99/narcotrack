package com.mayer99.narcotrack.base.models;


import com.mayer99.narcotrack.base.events.*;

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

    default void onEndOfInterval() {

    }

}
