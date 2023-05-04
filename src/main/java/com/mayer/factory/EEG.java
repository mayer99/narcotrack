package com.mayer.factory;


import java.nio.ByteBuffer;

public class EEG extends NarcotrackFrame {

    public EEG(int time, ByteBuffer buffer) {
        super (time, (byte)0xF1, 40);
        buffer.position(buffer.position() - length);
        buffer.get(raw);
        // Resetting buffer position to start
        buffer.position(buffer.position() - length);
    }

}
