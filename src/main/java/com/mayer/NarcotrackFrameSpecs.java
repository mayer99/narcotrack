package com.mayer;

import com.mayer.factory.*;

import java.nio.ByteBuffer;

public enum NarcotrackFrameSpecs {

    EEG((byte)0xF1, 40, com.mayer.factory.EEG.class),
    CURRENT_ASSESSMENT((byte)0xF2, 118, CurrentAssessment.class),
    POWER_SPECTRUM((byte)0xF3, 520, PowerSpectrum.class),
    ELECTRODE_CHECK((byte)0xF4, 28, ElectrodeCheck.class);

    private final byte identifier;
    private final int length;
    private final Class<? extends NarcotrackFrame> frame;

    NarcotrackFrameSpecs(byte identifier, int length, Class<? extends NarcotrackFrame> frame) {
        this.identifier = identifier;
        this.length = length;
        this.frame = frame;
    }

    public byte getIdentifier() {
        return identifier;
    }

    public int getLength() {
        return length;
    }

    public NarcotrackFrame createFrame(int time, ByteBuffer buffer) {
        try {
            frame.getDeclaredConstructors()[0].newInstance(time, buffer);
        } catch (Exception e) {
            System.out.println("Das ist schlecht");
        }
    }
}
