package com.mayer99.narcotrack.base.frames;

import com.mayer99.narcotrack.base.models.NarcotrackFrame;
import com.mayer99.narcotrack.base.models.NarcotrackFrameType;

import java.nio.ByteBuffer;

public class EEG extends NarcotrackFrame {

    public EEG(ByteBuffer buffer) {
        super (NarcotrackFrameType.EEG, buffer);
    }

}
