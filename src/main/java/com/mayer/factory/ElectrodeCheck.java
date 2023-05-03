package com.mayer.factory;

import java.nio.ByteBuffer;

public class ElectrodeCheck {

    private static final byte identifier = (byte)0xF4;
    private static final int length = 28;
    private final int time;
    private final byte[] raw;
    private final float imp1a, imp1b, impRef, imp2a, imp2b;
    private final byte info;
    private final byte[] chkSum;
    private static final byte  startByte = (byte)0xFF;

    public ElectrodeCheck(int time, ByteBuffer buffer) {
        this.time = time;
        buffer.position(buffer.position() - length);
        raw = new byte[length];
        buffer.get(raw);
        buffer.position(buffer.position() - length + 4);
        imp1a = buffer.getFloat();
        imp1b = buffer.getFloat();
        impRef = buffer.getFloat();
        imp2a = buffer.getFloat();
        imp2b = buffer.getFloat();
        info = buffer.get();
        chkSum = new byte[2];
        buffer.get(chkSum);
        // Resetting buffer position to start
        buffer.position(buffer.position() + 1 - length);
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

    public float getImp1a() {
        return imp1a;
    }

    public float getImp1b() {
        return imp1b;
    }

    public float getImpRef() {
        return impRef;
    }

    public float getImp2a() {
        return imp2a;
    }

    public float getImp2b() {
        return imp2b;
    }

    public byte getInfo() {
        return info;
    }

    public byte[] getChkSum() {
        return chkSum;
    }

    public static boolean detect(ByteBuffer buffer) {
        if(buffer.position() < length) return false;
        if(buffer.get(buffer.position() - length + 3) != identifier) return false;
        if(buffer.get(buffer.position() - length) != startByte) return false;
        return true;
    }
}
