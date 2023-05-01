package com.mayer.handler;

import com.mayer.NarcotrackListener;
import com.mayer.NarcotrackPackageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.sql.SQLException;

public class NarcotrackElectrodeCheckHandler extends NarcotrackFrameHandler {

    private static final Logger logger = LoggerFactory.getLogger(NarcotrackElectrodeCheckHandler.class);
    private final byte[] chkSum;
    public NarcotrackElectrodeCheckHandler(NarcotrackListener narcotracklistener) throws SQLException {
        super(narcotracklistener, NarcotrackPackageType.ELECTRODE_CHECK, "INSERT INTO electrode_check_frame(record_id, recorded_at, imp_1_a, imp_1_b, imp_ref, imp_2_a, imp_2_b, info, chk_sum, raw) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", false);
        chkSum = new byte[2];
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
            statement.setFloat(3, checkImpedance("1a", buffer.getFloat())); // imp_1_a
            statement.setFloat(4, checkImpedance("1b", buffer.getFloat())); // imp_1_b
            statement.setFloat(5, checkImpedance("ref", buffer.getFloat())); // imp_ref
            statement.setFloat(6, checkImpedance("2a", buffer.getFloat())); // imp_2_a
            statement.setFloat(7, checkImpedance("2b", buffer.getFloat())); // imp_2_b
            statement.setByte(8, buffer.get()); // info
            buffer.get(chkSum);
            statement.setBytes(9, chkSum); // chk_sum
            statement.setBytes(10, raw); // raw
            statement.executeUpdate();
            logger.debug("Sent Electrode Check");
        } catch (Exception e) {
            logger.error("Error processing Electrode Check data, maybe timeDifference: long cannot be cast to int", e);
        } finally {
            buffer.get();
            handleRemains(buffer);

        }
    }

    public float checkImpedance(String name, float value) {
        if (value < 0 || value > 49) {
            logger.error("{} electrode is probably disconnected ({})", name, value);
        } else if (value > 6) {
            logger.warn("{} electrode has a high impedance ({})", name, value);
        }
        return value;
    }
}
