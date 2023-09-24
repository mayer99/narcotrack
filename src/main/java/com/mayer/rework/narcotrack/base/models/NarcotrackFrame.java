package com.mayer.rework.narcotrack.base.models;

import java.nio.ByteBuffer;

public abstract class NarcotrackFrame {

    protected final int length;
    protected final byte[] raw;

    public NarcotrackFrame(NarcotrackFrameType frame, ByteBuffer buffer) {
        length = frame.getLength();
        raw = new byte[length];
        buffer.get(raw);
    }

    public byte[] getRaw() {
        return raw;
    }

}
