package com.mayer.factory;

import java.nio.ByteBuffer;

public class ElectrodeCheck {

    private static final byte identifier = (byte)0xF4;
    private static final int length = 28;
    private final int recordId;
    private final int time;
    private final byte[] raw;
    private final float imp1a, imp1b, impRef, imp2a, imp2b;
    private final byte info;
    private final byte[] chkSum;

    public ElectrodeCheck(int recordId, int time, ByteBuffer buffer) {
        this.recordId = recordId;
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
        buffer.get();
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
}
