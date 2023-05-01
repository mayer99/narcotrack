package com.mayer.handler;

import com.mayer.NarcotrackListener;
import com.mayer.NarcotrackPackageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.sql.SQLException;

public class NarcotrackEEGHandler extends NarcotrackFrameHandler {

    private static final Logger logger = LoggerFactory.getLogger(NarcotrackEEGHandler.class);
    private int batchCounter = 0;
    public NarcotrackEEGHandler(NarcotrackListener narcotracklistener) throws SQLException {
        super(narcotracklistener, NarcotrackPackageType.EEG, "INSERT INTO eeg_frame(record_id, recorded_at, raw) VALUES(?, ?, ?)", false);
    }

    @Override
    public void process(ByteBuffer buffer) {

        packageType.count();
        buffer.position(buffer.position() - packageType.getLength());
        buffer.get(raw);
        try {
            statement.setInt(1, narcotrackListener.getRecordId()); // record_id
            statement.setInt(2, getTimeDifference());
            statement.setBytes(3, raw);
            statement.addBatch();
            batchCounter++;
            logger.debug("Created EEG batch");

            if(batchCounter >= 0) {
                statement.executeBatch();
                batchCounter = 0;
                logger.debug("Send batches");
            }
        } catch (Exception e) {
            logger.error("Error processing EEG data, maybe timeDifference: long cannot be cast to int", e);
        } finally {
            logger.debug("finally statement in process");
            handleRemains(buffer);
        }
    }
}
