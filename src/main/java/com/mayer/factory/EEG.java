package com.mayer.factory;


import java.nio.ByteBuffer;

public class EEG {

    private static final byte identifier = (byte)0xF1;
    private static final int length = 40;
    private final int time;
    private final byte[] raw;
    private static final byte  startByte = (byte)0xFF;

    public EEG(int time, ByteBuffer buffer) {
        this.time = time;
        raw = new byte[length];
        buffer.position(buffer.position() - length);
        buffer.get(raw);
        // Resetting buffer position to start
        buffer.position(buffer.position() - length);
    }

    public static byte getIdentifier() {
        return identifier;
    }

    public static int getLength() {
        return length;
    }

    public int getTime() {
        return time;
    }

    public byte[] getRaw() {
        return raw;
    }

    public static boolean detect(ByteBuffer buffer) {
        if(buffer.position() < length) return false;
        if(buffer.get(buffer.position() - length + 3) != identifier) return false;
        if(buffer.get(buffer.position() - length) != startByte) return false;
        return true;
    }

}
