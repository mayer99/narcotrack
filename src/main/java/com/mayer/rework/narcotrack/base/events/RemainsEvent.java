package com.mayer.rework.narcotrack.base.events;

import com.mayer.rework.Narcotrack;
import com.mayer.rework.narcotrack.base.frames.Remains;
import com.mayer.rework.narcotrack.base.models.NarcotrackEvent;

public class RemainsEvent extends NarcotrackEvent {

    private final Remains remains;

    public RemainsEvent(long time, byte[] data) {
        super(time);
        remains = new Remains(data);
        Narcotrack.getHandlers().forEach(handler -> handler.onRemainsEvent(this));
    }

    public Remains getData() {
        return remains;
    }
}
