package com.mayer.frames;

import java.nio.ByteBuffer;

public class ElectrodeCheck extends NarcotrackFrame {

    private final float imp1a, imp1b, impRef, imp2a, imp2b;
    private final byte info;
    private final byte[] chkSum;

    public ElectrodeCheck(ByteBuffer buffer) {
        super(NarcotrackFrames.ELECTRODE_CHECK);
        buffer.position(buffer.position() - length);
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

}
