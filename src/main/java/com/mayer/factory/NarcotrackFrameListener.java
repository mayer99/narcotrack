package com.mayer.factory;

import com.mayer.Remains;
import com.mayer.frames.CurrentAssessment;
import com.mayer.frames.EEG;
import com.mayer.frames.ElectrodeCheck;
import com.mayer.frames.PowerSpectrum;

public interface NarcotrackFrameListener {

    default void onEEG(EEG data) {

    }

    default void onCurrentAssessment(CurrentAssessment data) {

    }

    default void onPowerSpectrum(PowerSpectrum data) {

    }

    default void onElectrodeCheck(ElectrodeCheck data) {

    }

    default void onRemains(Remains data) {

    }

}
