package com.mayer.old.frames;

import com.mayer.old.NarcotrackFrameType;

import java.nio.ByteBuffer;

public class EEG extends NarcotrackFrame {

    public EEG(ByteBuffer buffer) {
        super (NarcotrackFrameType.EEG, buffer);
    }

}
