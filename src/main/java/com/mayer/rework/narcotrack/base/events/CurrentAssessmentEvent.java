package com.mayer.rework.narcotrack.base.events;

import com.mayer.rework.Narcotrack;
import com.mayer.rework.narcotrack.base.frames.CurrentAssessment;
import com.mayer.rework.narcotrack.base.models.NarcotrackEvent;

import java.nio.ByteBuffer;

public class CurrentAssessmentEvent extends NarcotrackEvent {

    private final CurrentAssessment currentAssessment;

    public CurrentAssessmentEvent(long time, ByteBuffer buffer) {
        super(time);
        currentAssessment = new CurrentAssessment(buffer);
        Narcotrack.getHandlers().forEach(handler -> handler.onCurrentAssessmentEvent(this));
    }

    public CurrentAssessment getData() {
        return currentAssessment;
    }
}
