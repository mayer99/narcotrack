package com.mayer.narcotrack.core.frames;

import com.mayer.narcotrack.core.models.NarcotrackFrame;
import com.mayer.narcotrack.core.models.NarcotrackFrameType;

import java.nio.ByteBuffer;

public class EEG extends NarcotrackFrame {

    public EEG(ByteBuffer buffer) {
        super (NarcotrackFrameType.EEG, buffer);
    }

}
