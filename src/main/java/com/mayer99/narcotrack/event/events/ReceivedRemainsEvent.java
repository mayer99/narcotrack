package com.mayer99.narcotrack.event.events;

import com.mayer99.narcotrack.event.NarcotrackEvent;

import java.util.ArrayList;
import java.util.Arrays;

public class ReceivedRemainsEvent extends NarcotrackEvent {

    private final ArrayList<byte[]> chunks;

    public ReceivedRemainsEvent(int time, byte[] data) {
        super(time);
        chunks = new ArrayList<>();
        if (data.length <= 1000) {
            chunks.add(data);
            return;
        }
        for (int i = 0; i < data.length; i += 1000) {
            chunks.add(Arrays.copyOfRange(data, i, data.length - i > 1000 ? i + 1000 : data.length));
        }
    }

    public ArrayList<byte[]> getChunks() {
        return chunks;
    }

}
