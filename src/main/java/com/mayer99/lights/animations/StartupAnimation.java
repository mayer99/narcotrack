package com.mayer99.lights.animations;

import com.mayer99.lights.enums.StatusLight;
import com.mayer99.lights.enums.StatusLightColor;
import com.pi4j.io.spi.Spi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class StartupAnimation implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartupAnimation.class);
    private static final StatusLightColor COLOR = StatusLightColor.INFO;
    private static final int DURATION = 1400;
    private static final float OVERLAP = 0.1f;
    private static final float FADE_IN = 0.4f;
    private static final float PAUSE = 0.1f;
    private static final float FADE_OUT = 0.4f;
    private static final float OVERLAP_ADJUSTMENT = 1.0f - (StatusLight.values().length - 1) * OVERLAP;
    private static final float ADJ_FADE_IN = FADE_IN * OVERLAP_ADJUSTMENT;
    private static final float ADJ_PAUSE = PAUSE * OVERLAP_ADJUSTMENT;
    private static final float ADJ_FADE_OUT = FADE_OUT * OVERLAP_ADJUSTMENT;


    private final Spi spi;
    private final byte[] buffer;

    public StartupAnimation(Spi spi) {
        this.spi = spi;
        buffer = new byte[StatusLight.values().length * 3];
        Arrays.fill(buffer, (byte) 0);
    }

    @Override
    public void run() {
        try {
            int incrementDuration = Math.round((float) (DURATION / 100));
            while (!Thread.interrupted()) {
                for (float progress = 0; progress < 1.0f; progress+=0.01f) {
                    for (StatusLight light: StatusLight.values()) {
                        setStatusLight(light, getBrightness(light, progress));
                    }
                    spi.write(buffer);
                    Thread.sleep(incrementDuration);
                }
            }
        } catch (InterruptedException e) {
            LOGGER.debug("Die Animation wurde unterbrochen");
        }
    }

    private float getBrightness(StatusLight light, float progress) {
        float start = light.getIndex() * OVERLAP;
        if (progress <= start) return 0.0f;
        if (progress <= start + ADJ_FADE_IN) {
            return (progress - start) / ADJ_FADE_IN;
        }
        if (progress < start + ADJ_FADE_IN + ADJ_PAUSE) return 1.0f;
        if (progress < start + ADJ_FADE_IN + ADJ_PAUSE + ADJ_FADE_OUT) {
            return (1.0f - (progress - start - ADJ_FADE_IN - ADJ_PAUSE) / ADJ_FADE_OUT);
        }
        return 0.0f;
    }

    private void setStatusLight(StatusLight light, float brightness) {
        int i = light.getIndex() * 3;
        buffer[i] = (byte) (COLOR.getRed() * brightness);
        buffer[i + 1] = (byte) (COLOR.getBlue() * brightness);
        buffer[i + 2] = (byte) (COLOR.getGreen() * brightness);
    }
}
