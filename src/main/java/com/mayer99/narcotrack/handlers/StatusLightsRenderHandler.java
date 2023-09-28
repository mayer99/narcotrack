package com.mayer99.narcotrack.handlers;

import com.mayer99.Narcotrack;
import com.mayer99.lights.StatusLights;
import com.mayer99.narcotrack.base.models.NarcotrackEventHandler;

public class StatusLightsRenderHandler implements NarcotrackEventHandler {

    private final StatusLights statusLights;

    public StatusLightsRenderHandler(Narcotrack narcotrack) {
        statusLights = narcotrack.getStatusLights();
    }

    @Override
    public void onEndOfInterval() {
        statusLights.render();
    }
}
