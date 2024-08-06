package com.mayer99.lights.animations;

import com.mayer99.lights.StatusLightsDriver;

public interface StatusLightsAnimation {
    void run(StatusLightsDriver driver) throws InterruptedException;
}
