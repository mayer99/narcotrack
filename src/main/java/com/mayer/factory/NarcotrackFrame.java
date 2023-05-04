package com.mayer.factory;


public abstract class NarcotrackFrame {

    protected final int time;
    protected final byte identifier;
    protected final int length;
    protected final byte[] raw;

    public NarcotrackFrame(int time, byte identifier, int length) {
        this.time = time;
        this.identifier = identifier;
        this.length = length;
        raw = new byte[length];
    }

    public int getTime() {
        return time;
    }

    public byte[] getRaw() {
        return raw;
    }
}
