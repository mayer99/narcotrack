package com.mayer.events;

import com.mayer.Remains;
import com.mayer.factory.NarcotrackEventHandler;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class RemainsEvent extends NarcotrackEvent {

    public static final ArrayList<NarcotrackEventHandler> HANDLERS = new ArrayList<>();

    private final Remains remains;

    public RemainsEvent(int time, ByteBuffer buffer) {
        super(time);
        remains = new Remains(buffer);
        HANDLERS.forEach(handler -> handler.onRemainsEvent(this));
    }

    public RemainsEvent(int time, byte[] incomingData) {
        super(time);
        remains = new Remains(incomingData);
        HANDLERS.forEach(handler -> handler.onRemainsEvent(this));
    }


    @Override
    public ArrayList<NarcotrackEventHandler> getHandlers() {
        return HANDLERS;
    }

    public static ArrayList<NarcotrackEventHandler> getEventHandlers() {
        return HANDLERS;
    }

    public Remains getData() {
        return remains;
    }
}
