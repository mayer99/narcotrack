package com.mayer.rework.narcotrack.handlers;

import com.mayer.rework.Narcotrack;
import com.mayer.rework.lights.enums.StatusLight;
import com.mayer.rework.lights.enums.StatusLightColor;
import com.mayer.rework.lights.StatusLights;
import com.mayer.rework.narcotrack.base.models.NarcotrackEventHandler;

public class StatusLightsHandler implements NarcotrackEventHandler {

    private final StatusLights statusLights;

    public StatusLightsHandler(Narcotrack narcotrack) {
        statusLights = narcotrack.getStatusLights();
    }

    @Override
    public void onEndOfInterval() {
        statusLights.setPulseAnimation(StatusLight.STATUS, StatusLightColor.INFO);
        statusLights.render();
    }
}
