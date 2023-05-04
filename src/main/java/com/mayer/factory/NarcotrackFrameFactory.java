package com.mayer.factory;

import com.mayer.NarcotrackFrameSpecs;
import com.mayer.NarcotrackListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class NarcotrackFrameFactory {
    private static final Logger logger = LoggerFactory.getLogger(NarcotrackFrameFactory.class);
    private final byte  startByte = (byte)0xFF;


    public boolean containsFrames(ByteBuffer buffer) {

        if (buffer.position() < NarcotrackFrameSpecs.ELECTRODE_CHECK.getLength()) {
            logger.debug("Received fragment of {} bytes (warning: fragments longer than 28 bytes are still possible)", buffer.position());
            return false;
        }

        for (NarcotrackFrameSpecs specs: NarcotrackFrameSpecs.values()) {
            if (buffer.position() < )
        }

    }



}
