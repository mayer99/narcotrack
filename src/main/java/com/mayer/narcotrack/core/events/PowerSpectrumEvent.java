package com.mayer.narcotrack.core.events;

import com.mayer.narcotrack.core.Narcotrack;
import com.mayer.narcotrack.core.models.NarcotrackEvent;
import com.mayer.narcotrack.core.models.NarcotrackEventHandler;
import com.mayer.narcotrack.core.frames.PowerSpectrum;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class PowerSpectrumEvent extends NarcotrackEvent {

    private final PowerSpectrum powerSpectrum;

    public PowerSpectrumEvent(int time, ByteBuffer buffer) {
        super(time);
        powerSpectrum = new PowerSpectrum(buffer);
        Narcotrack.getHandlers().forEach(handler -> handler.onPowerSpectrumEvent(this));
    }

    public PowerSpectrum getData() {
        return powerSpectrum;
    }
}
