package com.mayer.rework.narcotrack.base.frames;

import com.mayer.rework.narcotrack.base.models.NarcotrackFrame;
import com.mayer.rework.narcotrack.base.models.NarcotrackFrameType;

import java.nio.ByteBuffer;

public class EEG extends NarcotrackFrame {

    public EEG(ByteBuffer buffer) {
        super (NarcotrackFrameType.EEG, buffer);
    }

}
