package com.mayer.factory;

import java.nio.ByteBuffer;

public enum NarcotrackFrame {

    EEG((byte)0xF1, 40),
    CURRENT_ASSESSMENT((byte)0xF2, 118),
    POWER_SPECTRUM((byte)0xF3, 520),
    ELECTRODE_CHECK((byte)0xF4, 28);

    private final byte identifier;
    private final int length;
    private final byte  startByte = (byte)0xFF;

    NarcotrackFrame(byte identifier, int length) {
        this.identifier = identifier;
        this.length = length;
    }

    public boolean detect(ByteBuffer buffer) {
        if(buffer.position() < length) return false;
        if(buffer.get(buffer.position() - length + 3) != identifier) return false;
        if(buffer.get(buffer.position() - length) != startByte) return false;
        return true;
    }

    public byte getIdentifier() {
        return identifier;
    }

    public int getLength() {
        return length;
    }

}
