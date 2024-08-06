package com.mayer99.lights;

import com.mayer99.lights.animations.ColorChangeAnimation;
import com.mayer99.lights.animations.StatusLightsAnimation;
import com.mayer99.lights.enums.StatusLightColor;

import java.util.concurrent.*;

public class StatusLights {

    private final ArrayBlockingQueue<StatusLightsAnimation> queue;
    private final StatusLightsDriver driver;
    private boolean disabled = false;

    public StatusLights() {
        queue = new ArrayBlockingQueue<>(20);
        driver = new StatusLightsDriver(queue);
        animate(new ColorChangeAnimation(StatusLightColor.BLUE));
    }

    public synchronized void animate(StatusLightsAnimation animation) {
        if (disabled) return;
        queue.offer(animation);
    }

    public synchronized void setColor(StatusLightColor color) {
        if (disabled) return;
        driver.setColor(color);
    }

    public synchronized void disable() {
        if (disabled) return;
        this.disabled = true;
        queue.clear();
        queue.offer(new ColorChangeAnimation(StatusLightColor.RED));
    }
}
