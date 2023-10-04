package com.mayer99.lights.tasks;

import com.mayer99.lights.enums.StatusLight;
import com.mayer99.lights.StatusLightController;
import com.mayer99.lights.models.StatusLightAnimation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;

public abstract class StatusLightAnimationTask implements Runnable {

    protected static final Logger LOGGER = LoggerFactory.getLogger(StatusLightAnimationTask.class);

    protected final StatusLightController controller;
    protected final StatusLight light;
    protected final ArrayBlockingQueue<StatusLightAnimation> queue;

    public StatusLightAnimationTask(StatusLightController controller, StatusLight light, ArrayBlockingQueue<StatusLightAnimation> queue) {
        this.controller = controller;
        this.light = light;
        this.queue = queue;
    }

}
