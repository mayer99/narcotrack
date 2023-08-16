package com.mayer.lights;

public enum StatusLightColor {
    OFF(0, 0, 0),
    INFO(0, 255, 0),
    WARNING(255, 70, 0),
    ERROR(255, 0, 0);

    private final int red;
    private final int green;
    private final int blue;

    StatusLightColor(int red, int green, int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
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
