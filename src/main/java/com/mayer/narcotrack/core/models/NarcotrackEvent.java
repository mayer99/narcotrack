package com.mayer.narcotrack.core.models;

public abstract class NarcotrackEvent {

    private final int time;

    public NarcotrackEvent(int time) {
        this.time = time;
    }

    public int getTime() {
        return time;
    }


}
