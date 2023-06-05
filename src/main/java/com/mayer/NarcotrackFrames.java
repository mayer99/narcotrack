package com.mayer;


public enum NarcotrackFrames {

    EEG((byte)0xF1, 40),
    CURRENT_ASSESSMENT((byte)0xF2, 118),
    POWER_SPECTRUM((byte)0xF3, 520),
    ELECTRODE_CHECK((byte)0xF4, 28);

    private final byte identifier;
    private final int length;
    private int count;

    NarcotrackFrames(byte identifier, int length) {
        this.identifier = identifier;
        this.length = length;
        count = 0;
    }

    public byte getIdentifier() {
        return identifier;
    }

    public int getLength() {
        return length;
    }

    public int getCount() {
        return count;
    }

    public void count() {
        count++;
    }

    public void resetCounter() {
        count = 0;
    }

}
