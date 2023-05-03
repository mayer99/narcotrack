package com.mayer.factory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class Remains {

    private final ArrayList<byte[]> data;
    private final int time;

    public Remains(int time, ByteBuffer buffer) {
        this.time = time;
        data = new ArrayList<>();
        final byte[] chunk = new byte[1000];
        int endPosition = buffer.position();
        buffer.position(0);
        while (endPosition - buffer.position() > 1000) {
            buffer.get(chunk);
            data.add(chunk);
        }
        if (endPosition - buffer.position() > 0) {
            byte[] lastChunk = new byte[endPosition - buffer.position()];
            buffer.get(lastChunk);
            data.add(lastChunk);
        }
        buffer.position(0);
    }

    public Remains(int time, byte[] incomingData) {
        this.time = time;
        data = new ArrayList<>();

        if (incomingData.length > 1000) {
            for (int i = 0; i < incomingData.length; i = i + 1000) {
                data.add(Arrays.copyOfRange(incomingData, i, incomingData.length - i > 1000 ? i + 1000 : incomingData.length - i));
            }
        }
        else {
            data.add(incomingData);
        }
    }

    public int getTime() {
        return time;
    }

    public ArrayList<byte[]> getData() {
        return data;
    }
}
