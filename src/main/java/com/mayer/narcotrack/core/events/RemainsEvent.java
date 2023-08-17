package com.mayer.narcotrack.core.events;

import com.mayer.narcotrack.core.Narcotrack;
import com.mayer.narcotrack.core.models.NarcotrackEvent;
import com.mayer.narcotrack.core.models.NarcotrackEventHandler;
import com.mayer.narcotrack.core.frames.Remains;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class RemainsEvent extends NarcotrackEvent {

    private final Remains remains;

    public RemainsEvent(int time, ByteBuffer buffer) {
        super(time);
        remains = new Remains(buffer);
        Narcotrack.getHandlers().forEach(handler -> handler.onRemainsEvent(this));
    }

    public RemainsEvent(int time, byte[] incomingData) {
        super(time);
        remains = new Remains(incomingData);
        Narcotrack.getHandlers().forEach(handler -> handler.onRemainsEvent(this));
    }

    public Remains getData() {
        return remains;
    }
}
