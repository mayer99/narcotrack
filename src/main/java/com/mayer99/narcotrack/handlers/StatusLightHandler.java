package com.mayer99.narcotrack.handlers;

import com.mayer99.lights.StatusLights;
import com.mayer99.lights.animations.ColorChangeAnimation;
import com.mayer99.lights.animations.RunningAnimation;
import com.mayer99.lights.enums.StatusLightColor;
import com.mayer99.narcotrack.base.models.NarcotrackEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class StatusLightHandler implements NarcotrackEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(StatusLightHandler.class);

    private final StatusLights statusLights;

    public StatusLightHandler() {
        logger.info("StatusLightHandler starting...");
        statusLights = new StatusLights();
    }

    @Override
    public void onRecordingStart(Instant instant) {
        statusLights.setColor(StatusLightColor.GREEN);
    }

    @Override
    public void onRecordingStop() {
        statusLights.animate(new ColorChangeAnimation(StatusLightColor.BLUE));
    }

    @Override
    public void onGoodElectrodes() {
        statusLights.setColor(StatusLightColor.GREEN);
    }

    @Override
    public void onLooseElectrode() {
        statusLights.setColor(StatusLightColor.ORANGE);
    }

    @Override
    public void onDetachedElectrode() {
        statusLights.setColor(StatusLightColor.RED);
    }

    @Override
    public void onCriticalError() {
        statusLights.disable();
    }

    @Override
    public void onEndOfInterval() {
        statusLights.animate(new RunningAnimation());
    }
}
