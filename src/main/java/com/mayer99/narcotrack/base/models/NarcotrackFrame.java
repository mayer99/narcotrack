package com.mayer99.narcotrack.base.models;

import java.nio.ByteBuffer;
import java.util.Arrays;

public abstract class NarcotrackFrame {


    protected final int length;
    protected final byte[] raw;
    protected final byte[] chkSum;
    protected final boolean isChecksumValid;

    public NarcotrackFrame(NarcotrackFrameType frame, ByteBuffer buffer) {
        length = frame.getLength();
        raw = new byte[length];
        buffer.get(raw);
        chkSum = Arrays.copyOfRange(raw, raw.length - 3, raw.length - 1);

        int calculatedCheckSum = 0;
        for (int i = 4; i < (raw.length - 3); i++) {
            calculatedCheckSum += (short) (raw[i]&0xff);
        }
        ByteBuffer checkSumBuffer = ByteBuffer.allocate(4).putInt(calculatedCheckSum).position(2);
        isChecksumValid = chkSum[1] == checkSumBuffer.get() && chkSum[0] == checkSumBuffer.get();
    }

    public byte[] getRaw() {
        return raw;
    }

    public byte[] getChkSum() {
        return chkSum;
    }

    public boolean isChecksumValid() {
        return isChecksumValid;
    }
}
