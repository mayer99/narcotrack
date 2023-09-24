package com.mayer99.narcotrack.base.frames;

import com.mayer99.narcotrack.base.models.NarcotrackFrame;
import com.mayer99.narcotrack.base.models.NarcotrackFrameType;

import java.nio.ByteBuffer;

public class PowerSpectrum extends NarcotrackFrame {

    private final int[] spectrum1, spectrum2;
    private final byte info;
    private final byte[] chkSum;

    public PowerSpectrum(ByteBuffer buffer) {
        super(NarcotrackFrameType.POWER_SPECTRUM, buffer);
        buffer.position(buffer.position() - length + 4);
        spectrum1 = new int[128];
        for (int i = 0; i < 128; i++) {
            spectrum1[i] = Short.toUnsignedInt(buffer.getShort());
        }
        spectrum2 = new int[128];
        for (int i = 0; i < 128; i++) {
            spectrum2[i] = Short.toUnsignedInt(buffer.getShort());
        }
        info = buffer.get();
        chkSum = new byte[2];
        buffer.get(chkSum);
    }

    public int[] getSpectrum1() {
        return spectrum1;
    }

    public int[] getSpectrum2() {
        return spectrum2;
    }

    public byte getInfo() {
        return info;
    }

    public byte[] getChkSum() {
        return chkSum;
    }

}
