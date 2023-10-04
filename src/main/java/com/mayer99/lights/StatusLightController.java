package com.mayer99.lights;

import com.mayer99.lights.models.StatusLightAnimation;
import com.mayer99.lights.enums.StatusLight;
import com.mayer99.lights.enums.StatusLightColor;
import com.mayer99.lights.tasks.ColorChangeAnimationTask;
import com.mayer99.lights.tasks.PulseAnimationTask;
import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.spi.Spi;
import com.pi4j.io.spi.SpiConfig;
import com.pi4j.io.spi.SpiMode;
import com.pi4j.library.pigpio.PiGpio;
import com.pi4j.plugin.pigpio.provider.spi.PiGpioSpiProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.concurrent.*;

public class StatusLightController {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatusLightController.class);
    private static final StatusLightColor STARTUP_COLOR = StatusLightColor.INFO;
    private static final int STARTUP_DURATION = 1500;
    private static final float STARTUP_OVERLAP = 0.1f;
    private static final float STARTUP_FADE_IN = 0.4f;
    private static final float STARTUP_PAUSE = 0.1f;
    private static final float STARTUP_FADE_OUT = 0.4f;


    private final byte[] buffer;
    private final EnumMap<StatusLight, ArrayBlockingQueue<StatusLightAnimation>> queues;
    private Spi spi;
    private boolean active;

    public StatusLightController() {

        buffer = new byte[StatusLight.values().length * 3];
        Arrays.fill(buffer, (byte) 0);
        queues = new EnumMap<>(StatusLight.class);
        try {
            PiGpio gpio = PiGpio.newNativeInstance();
            Context pi4j = Pi4J.newContextBuilder()
                    .noAutoDetect()
                    .add(PiGpioSpiProvider.newInstance(gpio))
                    .build();
            SpiConfig spiConfig = Spi.newConfigBuilder(pi4j)
                    .address(1)
                    .mode(SpiMode.MODE_0)
                    .baud(1_000_000)
                    .build();
            spi = pi4j.create(spiConfig);
            runStartupAnimation();
            ExecutorService executor = Executors.newFixedThreadPool(3);
            Arrays.stream(StatusLight.values()).forEach(light -> {
                ArrayBlockingQueue<StatusLightAnimation> queue = new ArrayBlockingQueue<>(10);
                queues.put(light, queue);
                executor.execute(light.equals(StatusLight.STATUS) ? new PulseAnimationTask(this, light, queue) : new ColorChangeAnimationTask(this, light, queue));
            });
            active = true;
        } catch (Exception e) {
            LOGGER.error("Could not initialize status lights", e);
            active = false;
        }
    }

    public synchronized void setStatusLight(StatusLight light, StatusLightColor color, float brightness) {
        int i = light.getIndex() * 3;
        buffer[i] = (byte) (color.getRed() * brightness);
        buffer[i + 1] = (byte) (color.getBlue() * brightness);
        buffer[i + 2] = (byte) (color.getGreen() * brightness);
        spi.write(buffer);
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            LOGGER.debug("Could no sleep for 1ms during execution of setStatusLight");
        }
    }

    public void animate(StatusLightAnimation animation) {
        if (!active) {
            LOGGER.debug("Did not perform animation, controller is not active");
            return;
        }
        if (!queues.get(animation.getLight()).offer(animation)) {
            LOGGER.error("Could not add animation with color {} to queue of {}", animation.getColor().name(), animation.getLight().name());
        }
    }

    private void runStartupAnimation() {
        int incrementDuration = Math.round((float) (STARTUP_DURATION / 100));
        float overlapAdjustment = 1.0f - (StatusLight.values().length - 1) * STARTUP_OVERLAP;
        float adjFadeIn = STARTUP_FADE_IN * overlapAdjustment;
        final float adjPause = STARTUP_PAUSE * overlapAdjustment;
        final float adjFadeOut = STARTUP_FADE_OUT * overlapAdjustment;
        try {
            for (int i = 0; i < 2; i++) {
                for (float progress = 0; progress < 1.0f; progress+=0.01f) {
                    for (StatusLight light: StatusLight.values()) {
                        float start = light.getIndex() * STARTUP_OVERLAP;
                        if (progress <= start) {
                            setStartupLight(light, 0.0f);
                            continue;
                        }
                        if (progress <= start + adjFadeIn) {
                            setStartupLight(light, (progress - start) / adjFadeIn);
                            continue;
                        }
                        if (progress < start + adjFadeIn + adjPause) {
                            setStartupLight(light, 1.0f);
                            continue;
                        }
                        if (progress < start + adjFadeIn + adjPause + adjFadeOut) {
                            setStartupLight(light, 1.0f - (progress - start - adjFadeIn - adjPause) / adjFadeOut);
                            continue;
                        }
                        setStartupLight(light, 0.0f);
                    }
                    spi.write(buffer);
                    Thread.sleep(incrementDuration);
                }
            }
        } catch (InterruptedException e) {
            LOGGER.debug("Interrupted startupAnimation", e);
        }
    }

    private void setStartupLight(StatusLight light, float brightness) {
        int i = light.getIndex() * 3;
        buffer[i] = (byte) (STARTUP_COLOR.getRed() * brightness);
        buffer[i + 1] = (byte) (STARTUP_COLOR.getBlue() * brightness);
        buffer[i + 2] = (byte) (STARTUP_COLOR.getGreen() * brightness);
    }

}
