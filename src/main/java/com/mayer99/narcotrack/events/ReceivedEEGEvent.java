package com.mayer99.narcotrack.events;

import java.nio.ByteBuffer;

public class ReceivedEEGEvent extends NarcotrackSerialDataEvent {

    public ReceivedEEGEvent(int time, ByteBuffer buffer) {
        super(time, buffer, NarcotrackFrameType.EEG);
    }

}
