package com.mayer.lights;

public class StatusLightAnimation {

    private final StatusLightAnimationType type;
    private final StatusLightColor color;

    public StatusLightAnimation(StatusLightAnimationType type, StatusLightColor color) {
        this.type = type;
        this.color = color;
    }

    public StatusLightAnimationType getType() {
        return type;
    }

    public StatusLightColor getColor() {
        return color;
    }
}
