package com.mayer99.lights;

import com.mayer99.lights.enums.StatusLight;
import com.mayer99.lights.enums.StatusLightAnimationType;
import com.mayer99.lights.enums.StatusLightColor;
import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.spi.Spi;
import com.pi4j.io.spi.SpiConfig;
import com.pi4j.io.spi.SpiMode;
import com.pi4j.library.pigpio.PiGpio;
import com.pi4j.plugin.pigpio.provider.spi.PiGpioSpiProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

public class StatusLights {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatusLights.class);

    private final ArrayBlockingQueue<Set<Map.Entry<StatusLight, StatusLightAnimation>>> animations;
    private final EnumMap<StatusLight, StatusLightAnimation> scheduledAnimations;
    private final boolean active;

    public StatusLights() {
        animations = new ArrayBlockingQueue<>(10);
        scheduledAnimations = new EnumMap<>(StatusLight.class);
        try {
            PiGpio gpio = PiGpio.newNativeInstance();
            Context pi4j = Pi4J.newContextBuilder()
                    .noAutoDetect()
                    .add(PiGpioSpiProvider.newInstance(gpio))
                    .build();
            SpiConfig spiConfig = Spi.newConfigBuilder(pi4j)
                    .address(0)
                    .mode(SpiMode.MODE_0)
                    .baud(1_000_000)
                    .build();
            Spi spi = pi4j.create(spiConfig);
            new StatusLightsAnimator(spi, animations);
        } catch (Exception e) {
            active = false;
            LOGGER.error("Could not initialize status lights", e);
            return;
        }
        active = true;
        LOGGER.info("StatusLights active");
    }

    public synchronized void setPulseAnimation(StatusLight statusLight, StatusLightColor color) {
        if (!active) return;
        scheduledAnimations.put(statusLight, new StatusLightAnimation( StatusLightAnimationType.PULSE, color));
    }

    public synchronized void setColorChangeAnimation(StatusLight statusLight, StatusLightColor color) {
        if (!active) return;
        scheduledAnimations.put(statusLight, new StatusLightAnimation( StatusLightAnimationType.COLOR_CHANGE, color));
    }

    public synchronized void render() {
        if (!active) return;
        animations.offer(scheduledAnimations.entrySet());
        scheduledAnimations.clear();
    }
}
