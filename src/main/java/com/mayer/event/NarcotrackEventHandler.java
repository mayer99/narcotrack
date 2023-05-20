package com.mayer.event;

import com.mayer.event.frame.CurrentAssessmentEvent;
import com.mayer.event.frame.EEGEvent;
import com.mayer.event.frame.ElectrodeCheckEvent;
import com.mayer.event.frame.PowerSpectrumEvent;
import com.mayer.event.remains.RemainsEvent;

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
