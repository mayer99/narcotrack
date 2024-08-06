package com.mayer99.lights.animations;

import com.mayer99.lights.StatusLightsDriver;
import com.mayer99.lights.enums.StatusLightColor;

public class RunningAnimation implements StatusLightsAnimation {

    private static final int DURATION = 2000;
    private static final int FREQUENCY = 60;
    private static final float FADE = 0.5f;
    private static final float ANIMATION = 0.3f;

    private static final int LED_COUNT = StatusLightsDriver.LED_COUNT;
    private static final int PERIOD = (int)(Math.floor(1000.0f / FREQUENCY));
    private static final float STEP = (float)(PERIOD) / DURATION;
    private static final float OVERLAP = (FADE - ANIMATION) / (LED_COUNT - 1.0f);

    private final StatusLightColor color;

    public RunningAnimation(StatusLightColor color) {
        this.color = color;
    }

    public RunningAnimation() {
        color = null;
    }

    @Override
    public void run(StatusLightsDriver driver) throws InterruptedException {
        if (color != null) driver.setColor(color);
        for (float progress = 0.0f; progress <= 1.0f; progress+= STEP) {
            for (int currentLed = 0; currentLed < LED_COUNT; currentLed++) {
                float startFadeIn = currentLed * OVERLAP;
                if (progress < startFadeIn) {
                    continue;
                }
                if (progress < startFadeIn + ANIMATION) {
                    driver.setBrightness(currentLed, (progress - startFadeIn) / ANIMATION);
                    continue;
                }
                float startFadeOut = 1.0f - (LED_COUNT - 1 - currentLed) * OVERLAP - ANIMATION;
                if (progress < startFadeOut) {
                    driver.setBrightness(currentLed, 1.0f);
                    continue;
                }
                if (progress < startFadeOut + ANIMATION) {
                    driver.setBrightness(currentLed, 1.0f - ((progress - startFadeOut) / ANIMATION));
                    continue;
                }
                driver.setBrightness(currentLed, 0.0f);
            }
            driver.render();
            Thread.sleep(PERIOD);
        }
        driver.setBrightness(0.0f);
        driver.render();
        Thread.sleep(PERIOD);
    }
}

