package com.mayer99.narcotrack.base.events;

import com.mayer99.Narcotrack;
import com.mayer99.narcotrack.base.frames.ElectrodeCheck;
import com.mayer99.narcotrack.base.models.NarcotrackEvent;

import java.nio.ByteBuffer;

public class ElectrodeCheckEvent extends NarcotrackEvent {

    private final ElectrodeCheck electrodeCheck;

    public ElectrodeCheckEvent(int time, ByteBuffer buffer) {
        super(time);
        electrodeCheck = new ElectrodeCheck(buffer);
        Narcotrack.getHandlers().forEach(handler -> handler.onElectrodeCheck(this));
    }

    public ElectrodeCheck getData() {
        return electrodeCheck;
    }
}
