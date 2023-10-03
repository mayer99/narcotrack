package com.mayer99.narcotrack.base.events;

import com.mayer99.Narcotrack;
import com.mayer99.narcotrack.base.frames.CurrentAssessment;
import com.mayer99.narcotrack.base.models.NarcotrackEvent;

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
