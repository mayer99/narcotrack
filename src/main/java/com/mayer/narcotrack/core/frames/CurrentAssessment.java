package com.mayer.narcotrack.core.frames;

import com.mayer.narcotrack.core.models.NarcotrackFrame;
import com.mayer.narcotrack.core.models.NarcotrackFrameType;

import java.nio.ByteBuffer;

public class CurrentAssessment extends NarcotrackFrame {

    private final short eegIndex, emgIndex;
    private final float deltaRel1, deltaRel2;
    private final float thetaRel1, thetaRel2;
    private final float alphaRel1, alphaRel2;
    private final float betaRel1, betaRel2;
    private final float power1, power2;
    private final float median1, median2;
    private final float edgeFreq1, edgeFreq2;
    private final byte[] artifacts1;
    private final byte[] artifacts2;
    private final byte alerts;
    private final byte[] info;
    private final float bsrShort1, bsrMedium1;
    private final byte[] reserved1;
    private final short stiCh1, stiCh2;
    private final float bsrShort2, bsrMedium2;
    private final short ibiCh1, ibiCh2;
    private final float aEEGMin1, aEEGMax1, aEEGMin2, aEEGMax2;
    private final byte[] reserved2;
    private final byte[] chkSum;

    public CurrentAssessment(ByteBuffer buffer) {
        super (NarcotrackFrameType.CURRENT_ASSESSMENT, buffer);
        buffer.position(buffer.position() - length + 4);
        eegIndex = buffer.getShort();
        emgIndex = buffer.getShort();
        deltaRel1 = buffer.getFloat();
        deltaRel2 = buffer.getFloat();
        thetaRel1 = buffer.getFloat();
        thetaRel2 = buffer.getFloat();
        alphaRel1 = buffer.getFloat();
        alphaRel2 = buffer.getFloat();
        betaRel1 = buffer.getFloat();
        betaRel2 = buffer.getFloat();
        power1 = buffer.getFloat();
        power2 = buffer.getFloat();
        median1 = buffer.getFloat();
        median2 = buffer.getFloat();
        edgeFreq1 = buffer.getFloat();
        edgeFreq2 = buffer.getFloat();
        artifacts1 = new byte[2];
        buffer.get(artifacts1);
        artifacts2 = new byte[2];
        buffer.get(artifacts2);
        alerts = buffer.get();
        info = new byte[2];
        buffer.get(info);
        bsrShort1 = buffer.getFloat();
        bsrMedium1 = buffer.getFloat();
        reserved1 = new byte[2];
        buffer.get(reserved1);
        stiCh1 = buffer.get();
        stiCh2 = buffer.get();
        bsrShort2 = buffer.getFloat();
        bsrMedium2 = buffer.getFloat();
        ibiCh1 = buffer.getShort();
        ibiCh2 = buffer.getShort();
        aEEGMin1 = buffer.getFloat();
        aEEGMax1 = buffer.getFloat();
        aEEGMin2 = buffer.getFloat();
        aEEGMax2 = buffer.getFloat();
        reserved2 = new byte[4];
        buffer.get(reserved2);
        chkSum = new byte[2];
        buffer.get(chkSum);
    }

    public short getEegIndex() {
        return this.eegIndex;
    }

    public short getEmgIndex() {
        return this.emgIndex;
    }

    public float getDeltaRel1() {
        return this.deltaRel1;
    }

    public float getDeltaRel2() {
        return this.deltaRel2;
    }

    public float getThetaRel1() {
        return this.thetaRel1;
    }

    public float getThetaRel2() {
        return this.thetaRel2;
    }

    public float getAlphaRel1() {
        return this.alphaRel1;
    }

    public float getAlphaRel2() {
        return this.alphaRel2;
    }

    public float getBetaRel1() {
        return this.betaRel1;
    }

    public float getBetaRel2() {
        return this.betaRel2;
    }

    public float getPower1() {
        return this.power1;
    }

    public float getPower2() {
        return this.power2;
    }

    public float getMedian1() {
        return this.median1;
    }

    public float getMedian2() {
        return this.median2;
    }

    public float getEdgeFreq1() {
        return this.edgeFreq1;
    }

    public float getEdgeFreq2() {
        return this.edgeFreq2;
    }

    public byte[] getArtifacts1() {
        return this.artifacts1;
    }

    public byte[] getArtifacts2() {
        return this.artifacts2;
    }

    public byte getAlerts() {
        return this.alerts;
    }

    public byte[] getInfo() {
        return this.info;
    }

    public float getBsrShort1() {
        return this.bsrShort1;
    }

    public float getBsrMedium1() {
        return this.bsrMedium1;
    }

    public byte[] getReserved1() {
        return this.reserved1;
    }

    public short getStiCh1() {
        return this.stiCh1;
    }

    public short getStiCh2() {
        return this.stiCh2;
    }

    public float getBsrShort2() {
        return this.bsrShort2;
    }

    public float getBsrMedium2() {
        return this.bsrMedium2;
    }

    public short getIbiCh1() {
        return this.ibiCh1;
    }

    public short getIbiCh2() {
        return this.ibiCh2;
    }

    public float getaEEGMin1() {
        return this.aEEGMin1;
    }

    public float getaEEGMax1() {
        return this.aEEGMax1;
    }

    public float getaEEGMin2() {
        return this.aEEGMin2;
    }

    public float getaEEGMax2() {
        return this.aEEGMax2;
    }

    public byte[] getReserved2() {
        return this.reserved2;
    }

    public byte[] getChkSum() {
        return this.chkSum;
    }

}
