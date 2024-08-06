package com.mayer99.narcotrack.base.events;

import com.mayer99.Narcotrack;
import com.mayer99.narcotrack.base.frames.EEG;
import com.mayer99.narcotrack.base.models.NarcotrackEvent;

import java.nio.ByteBuffer;

public class EEGEvent extends NarcotrackEvent {

    private final EEG eeg;

    public EEGEvent(int time, ByteBuffer buffer) {
        super(time);
        eeg = new EEG(buffer);
        Narcotrack.getHandlers().forEach(handler -> handler.onEEG(this));
    }

    public EEG getData() {
        return eeg;
    }
}
