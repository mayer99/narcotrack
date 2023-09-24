package com.mayer.rework.lights;

import com.mayer.rework.lights.enums.StatusLightAnimationType;
import com.mayer.rework.lights.enums.StatusLightColor;

public class StatusLightAnimation {

    private final StatusLightAnimationType type;
    private final StatusLightColor color;
    private final int fadeOutTime;

    public StatusLightAnimation(StatusLightAnimationType type, StatusLightColor color, int fadeOutTime) {
        this.type = type;
        this.color = color;
        this.fadeOutTime = fadeOutTime;
    }

    public StatusLightAnimation(StatusLightAnimationType type, StatusLightColor color) {
        this(type, color, 0);
    }

    public StatusLightAnimationType getType() {
        return type;
    }

    public StatusLightColor getColor() {
        return color;
    }

    public int getFadeOutTime() {
        return fadeOutTime;
    }

    public boolean isFadeOut() {
        return fadeOutTime > 0;
    }
}
