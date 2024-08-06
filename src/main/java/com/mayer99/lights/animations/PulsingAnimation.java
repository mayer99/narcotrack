package com.mayer99.lights.animations;

import com.mayer99.lights.StatusLightsDriver;
import com.mayer99.lights.enums.StatusLightColor;

public class PulsingAnimation implements StatusLightsAnimation {

    private static final int DURATION = 1500;
    private static final int PAUSE = 200;
    private static final int FREQUENCY = 60;

    private final int STEPS = (int)(Math.floor(((DURATION - PAUSE) / 1000.0f) * FREQUENCY * 0.5f));
    private final int PERIOD = (int)(Math.floor((DURATION - PAUSE) / (float)(STEPS)));

    private final StatusLightColor color;

    public PulsingAnimation(StatusLightColor color) {
        this.color = color;
    }

    public PulsingAnimation() {
        color = null;
    }

    @Override
    public void run(StatusLightsDriver driver) throws InterruptedException {
        if (color != null) driver.setColor(color);
        for (int step = 0; step < STEPS; step++) {
            driver.setBrightness(step/(float)(STEPS));
            driver.render();
            Thread.sleep(PERIOD);
        }
        Thread.sleep(PAUSE);
        for (int step = 0; step < STEPS; step++) {
            driver.setBrightness(1.0f - step/(float)(STEPS));
            driver.render();
            Thread.sleep(PERIOD);
        }
        driver.setBrightness(0.0f);
        driver.render();
        Thread.sleep(PERIOD);
    }
}
