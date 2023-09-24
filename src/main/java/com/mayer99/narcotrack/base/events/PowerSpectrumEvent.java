package com.mayer99.narcotrack.base.events;

import com.mayer99.Narcotrack;
import com.mayer99.narcotrack.base.frames.PowerSpectrum;
import com.mayer99.narcotrack.base.models.NarcotrackEvent;

import java.nio.ByteBuffer;

public class PowerSpectrumEvent extends NarcotrackEvent {

    private final PowerSpectrum powerSpectrum;

    public PowerSpectrumEvent(long time, ByteBuffer buffer) {
        super(time);
        powerSpectrum = new PowerSpectrum(buffer);
        Narcotrack.getHandlers().forEach(handler -> handler.onPowerSpectrumEvent(this));
    }

    public PowerSpectrum getData() {
        return powerSpectrum;
    }
}
