package com.mayer99.narcotrack;

public abstract class NarcotrackEvent {

    private final int time;

    public NarcotrackEvent(int time) {
        this.time = time;
    }

    public int getTime() {
        return time;
    }
}
