package com.mayer99.narcotrack.event.events;

import com.mayer99.narcotrack.data.NarcotrackFrame;
import com.mayer99.narcotrack.event.ReceivedFrameEvent;

import java.nio.ByteBuffer;

public class ReceivedPowerSpectrumEvent extends ReceivedFrameEvent {

    private final int[] spectrum1, spectrum2;
    private final byte info;

    public ReceivedPowerSpectrumEvent(int time, ByteBuffer buffer) {
        super(time, buffer, NarcotrackFrame.POWER_SPECTRUM);
        buffer.position(buffer.position() + 4);
        spectrum1 = new int[128];
        for (int i = 0; i < 128; i++) {
            spectrum1[i] = Short.toUnsignedInt(buffer.getShort());
        }
        spectrum2 = new int[128];
        for (int i = 0; i < 128; i++) {
            spectrum2[i] = Short.toUnsignedInt(buffer.getShort());
        }
        info = buffer.get();
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

}
