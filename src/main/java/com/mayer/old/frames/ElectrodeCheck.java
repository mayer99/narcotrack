package com.mayer.old.frames;

import com.mayer.old.NarcotrackFrameType;

import java.nio.ByteBuffer;

public class ElectrodeCheck extends NarcotrackFrame {

    private final float imp1a, imp1b, impRef, imp2a, imp2b;
    private final byte info;
    private final byte[] chkSum;

    public ElectrodeCheck(ByteBuffer buffer) {
        super(NarcotrackFrameType.ELECTRODE_CHECK, buffer);
        buffer.position(buffer.position() - length + 4);
        imp1a = buffer.getFloat();
        imp1b = buffer.getFloat();
        impRef = buffer.getFloat();
        imp2a = buffer.getFloat();
        imp2b = buffer.getFloat();
        info = buffer.get();
        chkSum = new byte[2];
        buffer.get(chkSum);
    }

    public String encodeHexString(byte[] byteArray) {
        StringBuilder hexStringBuffer = new StringBuilder();
        for (byte b : byteArray) {
            hexStringBuffer.append(byteToHex(b));
        }
        return hexStringBuffer.toString();
    }

    public String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits);
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
