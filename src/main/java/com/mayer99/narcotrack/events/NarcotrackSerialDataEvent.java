package com.mayer99.narcotrack.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;

public abstract class NarcotrackSerialDataEvent extends NarcotrackEvent {

    private static final Logger LOGGER = LoggerFactory.getLogger(NarcotrackSerialDataEvent.class);

    protected final int length;
    protected final byte[] raw;
    protected final byte[] checkSum;
    protected final boolean isChecksumValid;

    public NarcotrackSerialDataEvent(int time, ByteBuffer buffer, NarcotrackFrameType frameType) {
        super(time);
        length = frameType.getLength();
        raw = new byte[length];
        buffer.get(raw);
        checkSum = Arrays.copyOfRange(raw, raw.length - 3, raw.length - 1);

        int calculatedCheckSum = 0;
        for (int i = 4; i < (raw.length - 3); i++) {
            calculatedCheckSum += (short) (raw[i]&0xff);
        }
        ByteBuffer checkSumBuffer = ByteBuffer.allocate(4).putInt(calculatedCheckSum).position(2);
        isChecksumValid = checkSum[1] == checkSumBuffer.get() && checkSum[0] == checkSumBuffer.get();
        if (!isChecksumValid) {
            LOGGER.error("Received frame of type {} with invalid checksum at {} s", frameType, time);
        }
        buffer.reset();
    }

    public byte[] getRaw() {
        return raw;
    }

    public byte[] getCheckSum() {
        return checkSum;
    }

    public boolean isChecksumValid() {
        return isChecksumValid;
    }
}
