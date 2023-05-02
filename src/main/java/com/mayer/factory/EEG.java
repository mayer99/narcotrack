package com.mayer.factory;


import java.nio.ByteBuffer;

public class EEG {

    private static final byte identifier = (byte)0xF1;
    private static final int length = 40;
    private final int recordId;
    private final int time;
    private final byte[] raw;

    public EEG(int recordId, int time, ByteBuffer buffer) {
        this.recordId = recordId;
        this.time = time;
        raw = new byte[length];
        buffer.position(buffer.position() - length);
        buffer.get(raw);
    }

    public static byte getIdentifier() {
        return identifier;
    }

    public static int getLength() {
        return length;
    }

    public int getRecordId() {
        return recordId;
    }

    public int getTime() {
        return time;
    }

    public byte[] getRaw() {
        return raw;
    }

}
