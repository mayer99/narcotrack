package com.mayer99.narcotrack.events;

import java.nio.ByteBuffer;

public class ReceivedElectrodeCheckEvent extends NarcotrackSerialDataEvent {

    private final float imp1a, imp1b, impRef, imp2a, imp2b;
    private final byte info;

    public ReceivedElectrodeCheckEvent(int time, ByteBuffer buffer) {
        super(time, buffer, NarcotrackFrameType.ELECTRODE_CHECK);
        buffer.position(buffer.position() + 4);
        imp1a = buffer.getFloat();
        imp1b = buffer.getFloat();
        impRef = buffer.getFloat();
        imp2a = buffer.getFloat();
        imp2b = buffer.getFloat();
        info = buffer.get();
    }

    public float getImp1a() {
        return imp1a;
    }

    public float getImp1b() {
        return imp1b;
    }

    public float getImpRef() {
        return impRef;
    }

    public float getImp2a() {
        return imp2a;
    }

    public float getImp2b() {
        return imp2b;
    }

    public byte getInfo() {
        return info;
    }

}
