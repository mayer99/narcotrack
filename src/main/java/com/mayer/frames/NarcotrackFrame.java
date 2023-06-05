package com.mayer.frames;

import com.mayer.NarcotrackFrames;

public abstract class NarcotrackFrame {

    protected final int length;
    protected final byte[] raw;

    public NarcotrackFrame(NarcotrackFrames frame) {
        this.length = frame.getLength();
        raw = new byte[length];
    }

    public byte[] getRaw() {
        return raw;
    }

}
