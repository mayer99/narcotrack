package com.mayer.frames;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class Remains {

    private final ArrayList<byte[]> chunks;

    public Remains(ByteBuffer buffer) {
        chunks = new ArrayList<>();
        byte[] chunk = new byte[1000];
        int endPosition = buffer.position();
        buffer.position(0);
        while (endPosition - buffer.position() > 1000) {
            buffer.get(chunk);
            chunks.add(chunk);
        }
        if (endPosition - buffer.position() > 0) {
            byte[] lastChunk = new byte[endPosition - buffer.position()];
            buffer.get(lastChunk);
            chunks.add(lastChunk);
        }
        buffer.position(0);
    }

    public Remains(byte[] incomingData) {
        chunks = new ArrayList<>();

        if (incomingData.length > 1000) {
            for (int i = 0; i < incomingData.length; i = i + 1000) {
                chunks.add(Arrays.copyOfRange(incomingData, i, incomingData.length - i > 1000 ? i + 1000 : incomingData.length - i));
            }
        }
        else {
            chunks.add(incomingData);
        }
    }

    public ArrayList<byte[]> getChunks() {
        return chunks;
    }
}
