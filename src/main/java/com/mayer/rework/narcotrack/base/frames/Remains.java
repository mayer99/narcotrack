package com.mayer.rework.narcotrack.base.frames;

import java.util.ArrayList;
import java.util.Arrays;

public class Remains {

    private final ArrayList<byte[]> chunks;

    public Remains(byte[] incomingData) {
        chunks = new ArrayList<>();
        if (incomingData.length <= 1000) {
            chunks.add(incomingData);
            return;
        }
        for (int i = 0; i < incomingData.length; i += 1000) {
            chunks.add(Arrays.copyOfRange(incomingData, i, incomingData.length - i > 1000 ? i + 1000 : incomingData.length));
        }
    }

    public ArrayList<byte[]> getChunks() {
        return chunks;
    }
}
