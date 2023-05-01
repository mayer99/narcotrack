package com.mayer.handler;

import com.mayer.NarcotrackListener;
import com.mayer.NarcotrackPackageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.sql.*;
import java.util.Arrays;

public class NarcotrackPowerSpectrumHandler extends NarcotrackFrameHandler {

    private static final Logger logger = LoggerFactory.getLogger(NarcotrackPowerSpectrumHandler.class);
    private final byte[] chkSum;
    private final int[] spectrum1;
    private final int[] spectrum2;
    private PreparedStatement spectrumStatement;
    public NarcotrackPowerSpectrumHandler(NarcotrackListener narcotracklistener) throws SQLException {
        super(narcotracklistener, NarcotrackPackageType.POWER_SPECTRUM, "INSERT INTO power_spectrum_frame(record_id, recorded_at, info, chk_sum, raw) VALUES(?, ?, ?, ?, ?)", true);
        chkSum = new byte[2];
        spectrum1 = new int[128];
        spectrum2 = new int[128];
        spectrumStatement = narcotracklistener.getDatabaseConnection().prepareStatement("INSERT INTO power_spectrum(frame_id, channel, 0_0, 0_5, 1_0, 1_5, 2_0, 2_5, 3_0, 3_5, 4_0, 4_5, 5_0, 5_5, 6_0, 6_5, 7_0, 7_5, 8_0, 8_5, 9_0, 9_5, 10_0, 10_5, 11_0, 11_5, 12_0, 12_5, 13_0, 13_5, 14_0, 14_5, 15_0, 15_5, 16_0, 16_5, 17_0, 17_5, 18_0, 18_5, 19_0, 19_5, 20_0, 20_5, 21_0, 21_5, 22_0, 22_5, 23_0, 23_5, 24_0, 24_5, 25_0, 25_5, 26_0, 26_5, 27_0, 27_5, 28_0, 28_5, 29_0, 29_5, 30_0, 30_5, 31_0, 31_5, 32_0, 32_5, 33_0, 33_5, 34_0, 34_5, 35_0, 35_5, 36_0, 36_5, 37_0, 37_5, 38_0, 38_5, 39_0, 39_5, 40_0, 40_5, 41_0, 41_5, 42_0, 42_5, 43_0, 43_5, 44_0, 44_5, 45_0, 45_5, 46_0, 46_5, 47_0, 47_5, 48_0, 48_5, 49_0, 49_5, 50_0, 50_5, 51_0, 51_5, 52_0, 52_5, 53_0, 53_5, 54_0, 54_5, 55_0, 55_5, 56_0, 56_5, 57_0, 57_5, 58_0, 58_5, 59_0, 59_5, 60_0, 60_5, 61_0, 61_5, 62_0, 62_5, 63_0, 63_5) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?");
    }
    @Override
    public void process(ByteBuffer buffer) {

        packageType.count();
        buffer.position(buffer.position() - packageType.getLength());
        buffer.get(raw);
        buffer.position(buffer.position() - packageType.getLength() + 4);
        try {
            statement.setInt(1, narcotrackListener.getRecordId()); // record_id
            statement.setInt(2, getTimeDifference()); // recorded_at
            for (int i = 0; i < 128; i++) {
                spectrum1[i] = buffer.getShort();
            }
            for (int i = 0; i < 128; i++) {
                spectrum2[i] = buffer.getShort();
            }
            statement.setByte(3, buffer.get()); // info
            buffer.get(chkSum);
            statement.setBytes(4, chkSum); // chk_sum
            statement.setBytes(5, raw); // raw
            if(statement.executeUpdate() > 0) {
                ResultSet rs = statement.getGeneratedKeys();
                if(rs.next()) {
                    int frameId = rs.getInt(1);
                    insertSpectrum(frameId, 1, spectrum1);
                    insertSpectrum(frameId, 2, spectrum2);
                } else {
                    logger.error("Error processing power spectrum block. Result has keys, but resultset cannot be moved to next()");
                }
                rs.close();
            } else {
                logger.error("Error sending block for power spectrum. Did not receive keys as result");
            }
            logger.debug("Sent Power Spectrum");
        } catch (Exception e) {
            logger.error("Error processing Power Spectrum data, maybe timeDifference: long cannot be cast to int", e);
        } finally {
            buffer.get();
            handleRemains(buffer);
        }
    }

    private void insertSpectrum(int frameId, int channel, int[] spectrum) throws SQLException {

        if (Arrays.stream(spectrum).allMatch(value -> value == 0)) return;
        spectrumStatement.setInt(1, frameId);
        spectrumStatement.setInt(2, channel);
        for (int i = 0; i < spectrum.length; i++) {
            spectrumStatement.setInt(i + 3, spectrum[i]);
        }
        spectrumStatement.executeUpdate();
    }
}
