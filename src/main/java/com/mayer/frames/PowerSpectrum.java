package com.mayer.frames;

import java.nio.ByteBuffer;

public class PowerSpectrum extends NarcotrackFrame {

    private final int[] spectrum1, spectrum2;
    private final byte info;
    private final byte[] chkSum;

    public PowerSpectrum(int time, ByteBuffer buffer) {
        super(time, NarcotrackFrames.POWER_SPECTRUM);
        buffer.position(buffer.position() - length);
        buffer.get(raw);
        buffer.position(buffer.position() - length + 4);
        spectrum1 = new int[128];
        for (int i = 0; i < 128; i++) {
            spectrum1[i] = buffer.getShort();
        }
        spectrum2 = new int[128];
        for (int i = 0; i < 128; i++) {
            spectrum2[i] = buffer.getShort();
        }
        info = buffer.get();
        chkSum = new byte[2];
        buffer.get(chkSum);
        // Resetting buffer position to start
        buffer.position(buffer.position() + 1 - length);
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
