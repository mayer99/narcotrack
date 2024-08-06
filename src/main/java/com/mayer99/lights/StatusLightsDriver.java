package com.mayer99.lights;
import com.mayer99.lights.animations.ColorChangeAnimation;
import com.mayer99.lights.animations.FadeOutAnimation;
import com.mayer99.lights.animations.StatusLightsAnimation;
import com.mayer99.lights.enums.StatusLightColor;
import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.spi.Spi;
import com.pi4j.io.spi.SpiConfig;
import com.pi4j.library.pigpio.PiGpio;
import com.pi4j.plugin.pigpio.provider.spi.PiGpioSpiProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;

public class StatusLightsDriver implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(StatusLightsDriver.class);
    public static final int LED_COUNT = 8;
    private static final byte Bit_0 = (byte) 0b11000000;
    private static final byte Bit_1 = (byte) 0b11111000;
    private static final byte Bit_Reset = (byte) 0b00000000;

    private final ArrayBlockingQueue<StatusLightsAnimation> queue;
    private final Spi spi;
    protected final float[] brightness;
    private final byte[] raw;

    private StatusLightColor color = StatusLightColor.OFF;
    private Class<? extends StatusLightsAnimation> lastAnimation;
    private boolean disabled = false;

    public StatusLightsDriver(ArrayBlockingQueue<StatusLightsAnimation> queue) {
        this.queue = queue;
        PiGpio gpio = PiGpio.newNativeInstance();
        Context pi4j = Pi4J.newContextBuilder()
                .noAutoDetect()
                .add(PiGpioSpiProvider.newInstance(gpio))
                .build();
        SpiConfig spiConfig = Spi.newConfigBuilder(pi4j)
                .address(0)
                .baud(4_000_000)
                .build();
        spi = pi4j.create(spiConfig);
        brightness = new float[LED_COUNT];
        Arrays.fill(brightness, 0.0f);
        raw = new byte[LED_COUNT * 24 + 2];
        Arrays.fill(raw, Bit_0);
        raw[0] = Bit_Reset;
        raw[raw.length - 1] = Bit_Reset;
        Runtime.getRuntime().addShutdownHook(new StatusLightsShutdownHook());
        Thread thread = new Thread(this);
        thread.start();
    }

    class StatusLightsShutdownHook extends Thread {
        @Override
        public void run() {
            logger.info("StatusLightsShutdownHook triggered");
            if (spi == null) {
                logger.info("Spi is null");
                return;
            }
            logger.info("Attempting to turn off StatusLights");
            try {
                setDisabled();
                Arrays.fill(raw, Bit_0);
                raw[0] = Bit_Reset;
                raw[raw.length - 1] = Bit_Reset;
                spi.write(raw);
                logger.info("Turned off StatusLights");
            } catch (Exception e) {
                logger.error("Could not close StatusLights", e);
            }
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                StatusLightsAnimation animation = queue.take();
                if (queue.isEmpty()) {
                    if (lastAnimation != null && lastAnimation.equals(ColorChangeAnimation.class)) {
                        (new FadeOutAnimation()).run(this);
                    }
                    animation.run(this);
                    lastAnimation = animation.getClass();
                }
            } catch (Exception e) {
                logger.error("Interrupted", e);
            }
        }
    }

    private synchronized void setDisabled() {
        this.disabled = true;
    }

    public synchronized void render() {
        if (disabled) return;
        try {
            int counter = 1;
            for (float b: brightness) {
                int red = (int)(Math.floor(color.getRed() * b));
                int green = (int)(Math.floor(color.getGreen() * b));
                int blue = (int)(Math.floor(color.getBlue() * b));
                // Calculating GRB from RGB
                for (int j = 7; j >= 0; j--) {
                    if (((green >> j) & 1) == 1) {
                        raw[counter++] = Bit_1;
                    } else {
                        raw[counter++] = Bit_0;
                    }
                }
                for (int j = 7; j >= 0; j--) {
                    if (((red >> j) & 1) == 1) {
                        raw[counter++] = Bit_1;
                    } else {
                        raw[counter++] = Bit_0;
                    }
                }
                for (int j = 7; j >= 0; j--) {
                    if (((blue >> j) & 1) == 1) {
                        raw[counter++] = Bit_1;
                    } else {
                        raw[counter++] = Bit_0;
                    }
                }
            }
            spi.write(raw);
        } catch (Exception e) {
            logger.error("Unknown Exception rendering StatusLights", e);
        }
    }

    public synchronized void setBrightness(float brightness) {
        Arrays.fill(this.brightness, brightness);
    }

    public synchronized void setBrightness(int index, float brightness) {
        this.brightness[index] = brightness;
    }

    public synchronized void setColor(StatusLightColor color) {
        this.color = color;
    }

}

