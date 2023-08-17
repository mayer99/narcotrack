package com.mayer.narcotrack.core.events;

import com.mayer.narcotrack.core.Narcotrack;
import com.mayer.narcotrack.core.models.NarcotrackEvent;
import com.mayer.narcotrack.core.frames.CurrentAssessment;

import java.nio.ByteBuffer;

public class CurrentAssessmentEvent extends NarcotrackEvent {

    private final CurrentAssessment currentAssessment;

    public CurrentAssessmentEvent(int time, ByteBuffer buffer) {
        super(time);
        currentAssessment = new CurrentAssessment(buffer);
        Narcotrack.getHandlers().forEach(handler -> handler.onCurrentAssessmentEvent(this));
    }

    public CurrentAssessment getData() {
        return currentAssessment;
    }
}
