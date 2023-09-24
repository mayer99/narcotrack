package com.mayer.rework.narcotrack.base.events;

import com.mayer.rework.Narcotrack;
import com.mayer.rework.narcotrack.base.frames.ElectrodeCheck;
import com.mayer.rework.narcotrack.base.models.NarcotrackEvent;

import java.nio.ByteBuffer;

public class ElectrodeCheckEvent extends NarcotrackEvent {

    private final ElectrodeCheck electrodeCheck;

    public ElectrodeCheckEvent(long time, ByteBuffer buffer) {
        super(time);
        electrodeCheck = new ElectrodeCheck(buffer);
        Narcotrack.getHandlers().forEach(handler -> handler.onElectrodeCheckEvent(this));
    }

    public ElectrodeCheck getData() {
        return electrodeCheck;
    }
}
