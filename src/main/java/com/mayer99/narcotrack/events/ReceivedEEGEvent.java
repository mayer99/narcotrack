package com.mayer99.narcotrack.events;

import com.mayer99.narcotrack.NarcotrackFrameType;
import com.mayer99.narcotrack.NarcotrackSerialDataEvent;

import java.nio.ByteBuffer;

public class ReceivedEEGEvent extends NarcotrackSerialDataEvent {

    public ReceivedEEGEvent(int time, ByteBuffer buffer) {
        super(time, buffer, NarcotrackFrameType.EEG);
    }

}
