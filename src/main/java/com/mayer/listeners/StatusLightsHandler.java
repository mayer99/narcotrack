package com.mayer.listeners;

import com.mayer.Narcotrack;
import com.mayer.NarcotrackEventHandler;
import com.mayer.lights.StatusLight;
import com.mayer.lights.StatusLightColor;
import com.mayer.lights.StatusLights;

public class StatusLightsHandler implements NarcotrackEventHandler {

    private final StatusLights statusLights;

    public StatusLightsHandler(Narcotrack narcotrack) {
        statusLights = narcotrack.getStatusLights();
        Narcotrack.registerNarcotrackEventListener(this);
    }

    @Override
    public void onEndOfInterval() {
        statusLights.setPulseAnimation(StatusLight.STATUS, StatusLightColor.INFO);
        statusLights.render();
    }
}
