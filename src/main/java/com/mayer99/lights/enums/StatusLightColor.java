package com.mayer99.lights.enums;

public enum StatusLightColor {
    OFF(0, 0, 0),
    INFO(0, 255, 0),
    WARNING(255, 70, 0),
    ERROR(255, 0, 0);

    private final static float brightness = 0.3f;
    private final int red;
    private final int green;
    private final int blue;

    StatusLightColor(int red, int green, int blue) {
        this.red = Math.round(red * brightness);
        this.green = Math.round(green * brightness);
        this.blue = Math.round(blue * brightness);
    }

    public int getRed() {
        return red;
    }

    public int getGreen() {
        return green;
    }

    public int getBlue() {
        return blue;
    }
}
