package com.mayer.events;

import com.mayer.NarcotrackFrameType;

import java.util.ArrayList;

public abstract class NarcotrackEvent {

    private final int time;
    private final int frameCount;

    public NarcotrackEvent(int time, NarcotrackFrameType frameType) {
        this.time = time;
        frameCount = frameType.getCount();
    }

    public int getTime() {
        return time;
    }

    public int getFrameCount() {
        return frameCount;
    }

}
