package com.mayer;

public enum NarcotrackPackageType {

    EEG((byte)0xF1, 40),
    CURRENT_ASSESSMENT((byte)0xF2, 118),
    POWER_SPECTRUM((byte)0xF3, 520),
    ELECTRODE_CHECK((byte)0xF4, 28);


    private byte identifier;
    private int length;
    private int countTotal;
    private int countDifferential;
    NarcotrackPackageType(byte identifier, int length) {
        this.identifier = identifier;
        this.length = length;
        countTotal = 0;
        countDifferential = 0;
    }

    public void count() {
        countDifferential++;
        countTotal++;
        System.out.println("Something tried to count a package");
    }

    public void resetCounter() {
        countDifferential = 0;
    }

    public int getCountDifferential() {
        return countDifferential;
    }

    public int getCountTotal() {
        return countTotal;
    }

    public byte getIdentifier() {
        return identifier;
    }

    public int getLength() {
        return length;
    }
}
