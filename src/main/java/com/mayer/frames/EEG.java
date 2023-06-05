package com.mayer.frames;

import com.mayer.NarcotrackFrames;

import java.nio.ByteBuffer;

public class EEG extends NarcotrackFrame {

    public EEG(ByteBuffer buffer) {
        super (NarcotrackFrames.EEG);
        buffer.position(buffer.position() - length);
        buffer.get(raw);
        // Resetting buffer position to start
        buffer.position(buffer.position() - length);
    }

}
