package com.mayer.events;

import com.mayer.factory.NarcotrackEventHandler;

import java.util.ArrayList;

public abstract class NarcotrackEvent {

    private final int time;

    public NarcotrackEvent(int time) {
        this.time = time;
    }

    public int getTime() {
        return time;
    }

    public abstract ArrayList<NarcotrackEventHandler> getHandlers();

}
