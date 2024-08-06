package com.mayer99.lights.animations;

import com.mayer99.lights.StatusLightsDriver;
import com.mayer99.lights.enums.StatusLightColor;

public class FadeOutAnimation implements StatusLightsAnimation {

    private static final int DURATION = 700;
    private static final int FREQUENCY = 60;

    private static final int STEPS = (int)(Math.floor(1000.0f / FREQUENCY));
    private static final int PERIOD = (int)(Math.floor(DURATION / (float)(STEPS)));

    private final StatusLightColor color;

    public FadeOutAnimation(StatusLightColor color) {
        this.color = color;
    }

    public FadeOutAnimation() {
        color = null;
    }

    @Override
    public void run(StatusLightsDriver driver) throws InterruptedException {
        if (color != null) driver.setColor(color);
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
