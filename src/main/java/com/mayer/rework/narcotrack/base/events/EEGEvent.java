package com.mayer.rework.narcotrack.base.events;

import com.mayer.rework.Narcotrack;
import com.mayer.rework.narcotrack.base.frames.EEG;
import com.mayer.rework.narcotrack.base.models.NarcotrackEvent;

import java.nio.ByteBuffer;

public class EEGEvent extends NarcotrackEvent {

    private final EEG eeg;

    public EEGEvent(long time, ByteBuffer buffer) {
        super(time);
        eeg = new EEG(buffer);
        Narcotrack.getHandlers().forEach(handler -> handler.onEEGEvent(this));
    }

    public EEG getData() {
        return eeg;
    }
}
