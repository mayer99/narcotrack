package com.mayer.event.frame;

import com.mayer.event.NarcotrackEvent;
import com.mayer.event.NarcotrackEventHandler;
import com.mayer.frames.ElectrodeCheck;

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

    @Override
    public ArrayList<NarcotrackEventHandler> getHandlers() {
        return HANDLERS;
    }

    public static ArrayList<NarcotrackEventHandler> getEventHandlers() {
        return HANDLERS;
    }

    public ElectrodeCheck getData() {
        return electrodeCheck;
    }
}
