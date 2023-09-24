package com.mayer99.lights.enums;

public enum StatusLight {
    STATUS(0),
    ELECTRODES(1),
    NETWORK(2);

    private final int index;

    StatusLight(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
