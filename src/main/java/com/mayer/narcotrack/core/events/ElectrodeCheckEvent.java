package com.mayer.narcotrack.core.events;

import com.mayer.narcotrack.core.models.NarcotrackEventHandler;
import com.mayer.narcotrack.core.frames.ElectrodeCheck;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class ElectrodeCheckEvent extends NarcotrackEvent {

    private static final ArrayList<NarcotrackEventHandler> HANDLERS = new ArrayList<>();

    private final ElectrodeCheck electrodeCheck;

    public ElectrodeCheckEvent(int time, ByteBuffer buffer) {
        super(time);
        electrodeCheck = new ElectrodeCheck(buffer);
        HANDLERS.forEach(handler -> handler.onElectrodeCheckEvent(this));
    }

    public static ArrayList<NarcotrackEventHandler> getEventHandlers() {
        return HANDLERS;
    }

    public ElectrodeCheck getData() {
        return electrodeCheck;
    }
}
