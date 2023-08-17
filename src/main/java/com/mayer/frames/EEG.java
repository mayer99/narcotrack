package com.mayer.frames;

import java.nio.ByteBuffer;

public class EEG extends NarcotrackFrame {

    public EEG(ByteBuffer buffer) {
        super (NarcotrackFrameType.EEG, buffer);
    }

}
