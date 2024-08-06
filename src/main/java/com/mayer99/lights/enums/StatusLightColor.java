package com.mayer99.lights.enums;

public enum StatusLightColor {
    WHITE(0xFFFFFF),
    RED(0xFF0000),
    ORANGE(0xFFA500, 0.8f),
    YELLOW(0xFFFF00),
    GREEN(0x00FF00, 0.8f),
    LIGHT_BLUE(0xADD8E6),
    BLUE(0x0000FF, 0.8f),
    PURPLE(0x800080),
    PINK(0xFFC0CB),
    OFF(0x000000);

    private static final int RED_MASK = 0xFF0000;
    private static final int GREEN_MASK = 0x00FF00;
    private static final int BLUE_MASK = 0x0000FF;
    private static final float MAX_BRIGHTNESS = 0.8f;

    private final int red, green, blue;

    StatusLightColor(int color, float brightness) {
        red = (int) (((color & RED_MASK) >> 16) * brightness * MAX_BRIGHTNESS);
        green = (int) (((color & GREEN_MASK) >> 8) * brightness * MAX_BRIGHTNESS);
        blue = (int) ((color & BLUE_MASK) * brightness * MAX_BRIGHTNESS);
    }

    StatusLightColor(int color) {
        this(color, 1.0f);
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

