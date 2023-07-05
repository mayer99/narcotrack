package com.mayer.events;

import com.mayer.NarcotrackEventHandler;
import com.mayer.NarcotrackFrameType;
import com.mayer.frames.CurrentAssessment;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class CurrentAssessmentEvent extends NarcotrackEvent {

    private static final ArrayList<NarcotrackEventHandler> HANDLERS = new ArrayList<>();

    private final CurrentAssessment currentAssessment;

    public CurrentAssessmentEvent(int time, ByteBuffer buffer) {
        super(time, NarcotrackFrameType.CURRENT_ASSESSMENT);
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
