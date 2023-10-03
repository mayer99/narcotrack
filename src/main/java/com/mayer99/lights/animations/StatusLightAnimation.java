package com.mayer99.lights.animations;

import com.mayer99.lights.enums.StatusLight;
import com.mayer99.lights.enums.StatusLightColor;

public class StatusLightAnimation {

    private final StatusLight light;
    private final StatusLightColor color;

    public StatusLightAnimation(StatusLight light, StatusLightColor color) {
        this.light = light;
        this.color = color;
    }

    public StatusLight getLight() {
        return light;
    }

    public StatusLightColor getColor() {
        return color;
    }
}
