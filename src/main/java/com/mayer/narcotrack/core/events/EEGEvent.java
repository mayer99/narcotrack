package com.mayer.narcotrack.core.events;

import com.mayer.narcotrack.core.Narcotrack;
import com.mayer.narcotrack.core.models.NarcotrackEvent;
import com.mayer.narcotrack.core.models.NarcotrackEventHandler;
import com.mayer.narcotrack.core.frames.EEG;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class EEGEvent extends NarcotrackEvent {

    private final EEG eeg;

    public EEGEvent(int time, ByteBuffer buffer) {
        super(time);
        eeg = new EEG(buffer);
        Narcotrack.getHandlers().forEach(handler -> handler.onEEGEvent(this));
    }

    public EEG getData() {
        return eeg;
    }
}
