package com.mayer.narcotrack.core.events;

import com.mayer.narcotrack.core.models.NarcotrackEventHandler;
import com.mayer.narcotrack.core.frames.CurrentAssessment;

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

    public static ArrayList<NarcotrackEventHandler> getEventHandlers() {
        return HANDLERS;
    }

    public CurrentAssessment getData() {
        return currentAssessment;
    }
}
