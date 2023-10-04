package com.mayer99.lights.tasks;

import com.mayer99.lights.StatusLightController;
import com.mayer99.lights.models.StatusLightAnimation;
import com.mayer99.lights.enums.StatusLight;
import com.mayer99.lights.enums.StatusLightColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;

public class ColorChangeAnimationTask extends StatusLightAnimationTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(ColorChangeAnimationTask.class);
    private static final int DURATION = 800;
    private static final int PAUSE = 200;
    private static final int FREQUENCY = 30;

    private StatusLightColor color = StatusLightColor.OFF;

    public ColorChangeAnimationTask(StatusLightController controller, StatusLight light, ArrayBlockingQueue<StatusLightAnimation> queue) {
        super(controller, light, queue);
    }

    @Override
    public void run() {
        while (true) {
            int incrementDuration = Math.round(((float) ((DURATION - PAUSE) / FREQUENCY)) * 0.5f);
            try {
                StatusLightAnimation animation = queue.take();
                if (color.equals(animation.getColor())) {
                    LOGGER.debug("Skipping ColorChange animation of {} to {}, light already has the same color", animation.getLight().name(), animation.getColor().name());
                    continue;
                }
                for (int i = 0; i < FREQUENCY; i++) {
                    controller.setStatusLight(light, color, 1.0f - i / (FREQUENCY - 1.0f));
                    Thread.sleep(incrementDuration);
                }
                color = animation.getColor();
                Thread.sleep(PAUSE);
                for (int i = 0; i < FREQUENCY; i++) {
                    controller.setStatusLight(light, animation.getColor(), i / (FREQUENCY - 1.0f));
                    Thread.sleep(incrementDuration);
                }
            } catch (InterruptedException e) {
                LOGGER.debug("ColorChangeAnimationTask has been interrupted");
            }
        }
    }
}
