package com.mayer.events;

import com.mayer.NarcotrackEventHandler;
import com.mayer.frames.EEG;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class EEGEvent extends NarcotrackEvent {

    private static final ArrayList<NarcotrackEventHandler> HANDLERS = new ArrayList<>();

    private final EEG eeg;

    public EEGEvent(int counter, int time, ByteBuffer buffer) {
        super(time);
        eeg = new EEG(buffer);
        HANDLERS.forEach(handler -> handler.onEEGEvent(this));
    }

    public static ArrayList<NarcotrackEventHandler> getEventHandlers() {
        return HANDLERS;
    }

    public EEG getData() {
        return eeg;
    }
}
