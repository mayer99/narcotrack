package com.mayer99.narcotrack.event.events;

import com.mayer99.narcotrack.data.NarcotrackFrame;
import com.mayer99.narcotrack.event.ReceivedFrameEvent;

import java.nio.ByteBuffer;

public class ReceivedEEGEvent extends ReceivedFrameEvent {

    public ReceivedEEGEvent(int time, ByteBuffer buffer) {
        super(time, buffer, NarcotrackFrame.EEG);
    }

}
