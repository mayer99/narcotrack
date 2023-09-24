package com.mayer99.lights;

import com.mayer99.lights.enums.StatusLight;
import com.mayer99.lights.enums.StatusLightAnimationType;
import com.mayer99.lights.enums.StatusLightColor;
import com.pi4j.io.spi.Spi;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

public class StatusLightsAnimator implements Runnable {

    private static final int STARTUP_ANIMATION_LIGHT_OVERLAP = 6;
    private static final long STARTUP_ANIMATION_FRAME_DURATION = 25;
    private static final long STARTUP_ANIMATION_PAUSE_DURATION = 100;
    private static final StatusLightColor STARTUP_ANIMATION_COLOR = StatusLightColor.INFO;
    private static final int ANIMATION_FRAMES = 20;
    private static final long ANIMATION_FRAME_DURATION = 25;
    private static final long ANIMATION_PAUSE_DURATION = 400;

    private final Spi spi;
    private final ArrayBlockingQueue<Set<Map.Entry<StatusLight, StatusLightAnimation>>> animationQueue;
    private final EnumMap<StatusLight, StatusLightColor> state;
    private final byte[] buffer;

    public StatusLightsAnimator(Spi spi, ArrayBlockingQueue<Set<Map.Entry<StatusLight, StatusLightAnimation>>> animationQueue) {
        this.spi = spi;
        this.animationQueue = animationQueue;
        this.buffer = new byte[StatusLight.values().length * 3];
        Arrays.fill(buffer, (byte) 0);
        state = new EnumMap<>(StatusLight.class);
        for (StatusLight statusLight: StatusLight.values()) {
            state.put(statusLight, StatusLightColor.OFF);
        }
        Thread thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        try {
            runStartupAnimation();
        } catch (InterruptedException ignored) {
        }
        while(true) {
            try {
                Set<Map.Entry<StatusLight, StatusLightAnimation>> animations = animationQueue.take();
                // First phase
                for (int i = 0; i < ANIMATION_FRAMES; i++) {
                    float brightness = i / (ANIMATION_FRAMES - 1.0f);
                    for (Map.Entry<StatusLight, StatusLightAnimation> animation: animations) {
                        if (state.get(animation.getKey()).equals(animation.getValue().getColor()) && animation.getValue().getType().equals(StatusLightAnimationType.COLOR_CHANGE)) return;
                        if (state.get(animation.getKey()).equals(StatusLightColor.OFF)) {
                            setStatusLight(animation.getKey(), animation.getValue().getColor(), brightness);
                        } else {
                            setStatusLight(animation.getKey(), state.get(animation.getKey()), 1 - brightness);
                        }
                    }
                    spi.write(buffer);
                    Thread.sleep(ANIMATION_FRAME_DURATION);
                }
                Thread.sleep(ANIMATION_PAUSE_DURATION);

                // Second phase
                for (int i = 0; i < ANIMATION_FRAMES; i++) {
                    float brightness = i / (ANIMATION_FRAMES - 1.0f);
                    for (Map.Entry<StatusLight, StatusLightAnimation> animation: animations) {
                        if (state.get(animation.getKey()).equals(animation.getValue().getColor()) && animation.getValue().getType().equals(StatusLightAnimationType.COLOR_CHANGE)) return;
                        if (!state.get(animation.getKey()).equals(StatusLightColor.OFF)) {
                            setStatusLight(animation.getKey(), animation.getValue().getColor(), brightness);
                        } else if (animation.getValue().getType().equals(StatusLightAnimationType.PULSE)) {
                            setStatusLight(animation.getKey(), animation.getValue().getColor(), 1 - brightness);
                        }
                    }
                    spi.write(buffer);
                    Thread.sleep(ANIMATION_FRAME_DURATION);
                }

                // Third phase
                if (animations.stream().anyMatch(animation -> !state.get(animation.getKey()).equals(StatusLightColor.OFF) && animation.getValue().getType().equals(StatusLightAnimationType.PULSE))) {
                    Thread.sleep(ANIMATION_PAUSE_DURATION);
                    for (int i = 0; i < ANIMATION_FRAMES; i++) {
                        float brightness = i / (ANIMATION_FRAMES - 1.0f);
                        for (Map.Entry<StatusLight, StatusLightAnimation> animation: animations) {
                            if (!state.get(animation.getKey()).equals(StatusLightColor.OFF) && animation.getValue().getType().equals(StatusLightAnimationType.PULSE)) {
                                setStatusLight(animation.getKey(), animation.getValue().getColor(), 1 - brightness);
                            }
                        }
                        spi.write(buffer);
                        Thread.sleep(ANIMATION_FRAME_DURATION);
                    }
                }

                for (Map.Entry<StatusLight, StatusLightAnimation> animation: animations) {
                    state.replace(animation.getKey(), animation.getValue().getType().equals(StatusLightAnimationType.COLOR_CHANGE) ? animation.getValue().getColor() : StatusLightColor.OFF);
                }
            } catch (InterruptedException ignored) {
            }
        }
    }

    public void runStartupAnimation() throws InterruptedException {
        int frameCount = (StatusLight.values().length - 1) * STARTUP_ANIMATION_LIGHT_OVERLAP + ANIMATION_FRAMES; // Frames von 0 bis 31
        while(animationQueue.isEmpty()) {
            for (int i = 0; i < frameCount; i++) {
                for (StatusLight statusLight: StatusLight.values()) {
                    int start = statusLight.getIndex() * STARTUP_ANIMATION_LIGHT_OVERLAP;
                    if (i >= start && i < start + ANIMATION_FRAMES) {
                        float brightness = (i - start) / (ANIMATION_FRAMES - 1.0f);
                        setStatusLight(statusLight, STARTUP_ANIMATION_COLOR, brightness);
                    }
                }
                spi.write(buffer);
                Thread.sleep(STARTUP_ANIMATION_FRAME_DURATION);
            }
            Thread.sleep(STARTUP_ANIMATION_PAUSE_DURATION);
            for (int i = 0; i < frameCount; i++) {
                for (StatusLight statusLight: StatusLight.values()) {
                    int start = statusLight.getIndex() * STARTUP_ANIMATION_LIGHT_OVERLAP;
                    if (i >= start && i < start + ANIMATION_FRAMES) {
                        float brightness = 1 - ((i - start) / (ANIMATION_FRAMES - 1.0f));
                        setStatusLight(statusLight, STARTUP_ANIMATION_COLOR, brightness);
                    }
                }
                spi.write(buffer);
                Thread.sleep(STARTUP_ANIMATION_FRAME_DURATION);
            }
            Thread.sleep(STARTUP_ANIMATION_PAUSE_DURATION);
        }
    }

    private void setStatusLight(StatusLight statusLight, StatusLightColor color, float brightness) {
        buffer[statusLight.getIndex() * 3] = (byte) (color.getRed() * brightness);
        buffer[statusLight.getIndex() * 3 + 1] = (byte) (color.getBlue() * brightness);
        buffer[statusLight.getIndex() * 3 + 2] = (byte) (color.getGreen() * brightness);
    }

}
