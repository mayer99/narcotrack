package com.mayer.frames;


import com.mayer.frames.NarcotrackFrames;

public abstract class NarcotrackFrame {

    protected final int time;
    protected final int length;
    protected final byte[] raw;

    public NarcotrackFrame(int time, NarcotrackFrames frame) {
        this.time = time;
        this.length = frame.getLength();
        raw = new byte[length];
    }

    public int getTime() {
        return time;
    }

    public byte[] getRaw() {
        return raw;

    }

}
