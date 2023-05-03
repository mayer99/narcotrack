package com.mayer.factory;

import java.nio.ByteBuffer;

public class PowerSpectrum {

    private static final byte identifier = (byte)0xF3;
    private static final int length = 520;
    private final int time;
    private final byte[] raw;
    private final int[] spectrum1, spectrum2;
    private final byte info;
    private final byte[] chkSum;
    private static final byte  startByte = (byte)0xFF;

    public PowerSpectrum(int time, ByteBuffer buffer) {
        this.time = time;
        buffer.position(buffer.position() - length);
        raw = new byte[length];
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

    public static boolean detect(ByteBuffer buffer) {
        if(buffer.position() < length) return false;
        if(buffer.get(buffer.position() - length + 3) != identifier) return false;
        if(buffer.get(buffer.position() - length) != startByte) return false;
        return true;
    }
}
