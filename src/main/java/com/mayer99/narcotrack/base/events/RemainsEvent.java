package com.mayer99.narcotrack.base.events;

import com.mayer99.Narcotrack;
import com.mayer99.narcotrack.base.frames.Remains;
import com.mayer99.narcotrack.base.models.NarcotrackEvent;

public class RemainsEvent extends NarcotrackEvent {

    private final Remains remains;

    public RemainsEvent(int time, byte[] data) {
        super(time);
        remains = new Remains(data);
        Narcotrack.getHandlers().forEach(handler -> handler.onRemainsEvent(this));
    }

    public Remains getData() {
        return remains;
    }
}
