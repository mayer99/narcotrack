package com.mayer.frames;


public enum NarcotrackFrames {

    EEG((byte)0xF1, 40),
    CURRENT_ASSESSMENT((byte)0xF2, 128),
    POWER_SPECTRUM((byte)0xF3, 520),
    ELECTRODE_CHECK((byte)0xF4, 28);

    private final byte identifier;
    private final int length;

    NarcotrackFrames(byte identifier, int length) {
        this.identifier = identifier;
        this.length = length;
    }

    public byte getIdentifier() {
        return identifier;
    }

    public int getLength() {
        return length;
    }

}
