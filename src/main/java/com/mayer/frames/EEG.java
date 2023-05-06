package com.mayer.frames;

import com.mayer.factory.NarcotrackFrameListener;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class EEG extends NarcotrackFrame {

    private static final ArrayList<NarcotrackFrameListener> listeners = new ArrayList<>();

    public EEG(int time, ByteBuffer buffer) {
        super (time, NarcotrackFrames.EEG);
        buffer.position(buffer.position() - length);
        buffer.get(raw);
        // Resetting buffer position to start
        buffer.position(buffer.position() - length);
    }

    public static ArrayList<NarcotrackFrameListener> getListeners() {
        return listeners;
    }

    public static void addListener(NarcotrackFrameListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }


}
