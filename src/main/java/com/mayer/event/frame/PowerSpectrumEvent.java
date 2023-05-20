package com.mayer.event.frame;

import com.mayer.event.NarcotrackEvent;
import com.mayer.event.NarcotrackEventHandler;
import com.mayer.frames.PowerSpectrum;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class PowerSpectrumEvent extends NarcotrackEvent {

    private static final ArrayList<NarcotrackEventHandler> HANDLERS = new ArrayList<>();

    private final PowerSpectrum powerSpectrum;

    public PowerSpectrumEvent(int time, ByteBuffer buffer) {
        super(time);
        powerSpectrum = new PowerSpectrum(buffer);
        HANDLERS.forEach(handler -> handler.onPowerSpectrumEvent(this));
    }

    @Override
    public ArrayList<NarcotrackEventHandler> getHandlers() {
        return null;
    }

    public static ArrayList<NarcotrackEventHandler> getEventHandlers() {
        return HANDLERS;
    }

    public PowerSpectrum getData() {
        return powerSpectrum;
    }
}
