package com.mayer.narcotrack.core.events;

import com.mayer.narcotrack.core.Narcotrack;
import com.mayer.narcotrack.core.models.NarcotrackEvent;
import com.mayer.narcotrack.core.models.NarcotrackEventHandler;
import com.mayer.narcotrack.core.frames.ElectrodeCheck;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class ElectrodeCheckEvent extends NarcotrackEvent {

    private final ElectrodeCheck electrodeCheck;

    public ElectrodeCheckEvent(int time, ByteBuffer buffer) {
        super(time);
        electrodeCheck = new ElectrodeCheck(buffer);
        Narcotrack.getHandlers().forEach(handler -> handler.onElectrodeCheckEvent(this));
    }

    public ElectrodeCheck getData() {
        return electrodeCheck;
    }
}
