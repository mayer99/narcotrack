package com.mayer.narcotrack.handler;

import com.mayer.narcotrack.core.Narcotrack;
import com.mayer.lights.StatusLight;
import com.mayer.lights.StatusLightColor;
import com.mayer.lights.StatusLights;
import com.mayer.narcotrack.core.models.NarcotrackEventHandler;

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
