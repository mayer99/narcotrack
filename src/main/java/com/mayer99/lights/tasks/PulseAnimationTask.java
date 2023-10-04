package com.mayer99.lights.tasks;

import com.mayer99.lights.models.StatusLightAnimation;
import com.mayer99.lights.enums.StatusLight;
import com.mayer99.lights.StatusLightController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;

public class PulseAnimationTask extends StatusLightAnimationTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(PulseAnimationTask.class);
    private static final int DURATION = 800;
    private static final int PAUSE = 200;
    private static final int FREQUENCY = 30;

    public PulseAnimationTask(StatusLightController controller, StatusLight light, ArrayBlockingQueue<StatusLightAnimation> queue) {
        super(controller, light, queue);
    }

    @Override
    public void run() {
        while (true) {
            try {
                StatusLightAnimation animation = queue.take();
                int incrementDuration = Math.round(((float) ((DURATION - PAUSE) / FREQUENCY)) * 0.5f);
                for (int i = 0; i < FREQUENCY; i++) {
                    controller.setStatusLight(light, animation.getColor(), i / (FREQUENCY - 1.0f));
                    Thread.sleep(incrementDuration);
                }
                Thread.sleep(PAUSE);
                for (int i = 0; i < FREQUENCY; i++) {
                    controller.setStatusLight(light, animation.getColor(), 1.0f - i / (FREQUENCY - 1.0f));
                    Thread.sleep(incrementDuration);
                }
            } catch (InterruptedException e) {
                LOGGER.debug("PulseAnimationTask has been interrupted");
            }
        }
    }
}
