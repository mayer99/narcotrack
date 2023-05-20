package com.mayer.event.frame;

import com.mayer.event.NarcotrackEvent;
import com.mayer.event.NarcotrackEventHandler;
import com.mayer.frames.CurrentAssessment;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class CurrentAssessmentEvent extends NarcotrackEvent {

    private static final ArrayList<NarcotrackEventHandler> HANDLERS = new ArrayList<>();

    private final CurrentAssessment currentAssessment;

    public CurrentAssessmentEvent(int time, ByteBuffer buffer) {
        super(time);
        currentAssessment = new CurrentAssessment(buffer);
        HANDLERS.forEach(handler -> handler.onCurrentAssessmentEvent(this));
    }

    @Override
    public ArrayList<NarcotrackEventHandler> getHandlers() {
        return HANDLERS;
    }

    public static ArrayList<NarcotrackEventHandler> getEventHandlers() {
        return HANDLERS;
    }

    public CurrentAssessment getData() {
        return currentAssessment;
    }
}
