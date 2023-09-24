package com.mayer99.narcotrack.handlers;

import com.mayer99.Narcotrack;
import com.mayer99.lights.enums.StatusLight;
import com.mayer99.lights.enums.StatusLightColor;
import com.mayer99.lights.StatusLights;
import com.mayer99.narcotrack.base.models.NarcotrackEventHandler;

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
