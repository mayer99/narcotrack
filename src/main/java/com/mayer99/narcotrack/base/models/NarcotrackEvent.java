package com.mayer99.narcotrack.base.models;

public abstract class NarcotrackEvent {

    private final long time;

    public NarcotrackEvent(long time) {
        this.time = time;
    }

    public long getTime() {
        return time;
    }


}
