package com.mayer.handler;

import com.mayer.NarcotrackListener;
import com.mayer.NarcotrackPackageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.sql.SQLException;

public class NarcotrackCurrentAssessmentHandler extends NarcotrackFrameHandler {

    private static final Logger logger = LoggerFactory.getLogger(NarcotrackCurrentAssessmentHandler.class);
    private final byte[] artifacts1;
    private final byte[] artifacts2;
    private final byte[] info;
    private final byte[] reserved1;
    private final byte[] reserved2;
    private final byte[] chkSum;
    private int batchCounter = 0;
    public NarcotrackCurrentAssessmentHandler(NarcotrackListener narcotracklistener) throws SQLException {
        super(narcotracklistener, NarcotrackPackageType.CURRENT_ASSESSMENT, "INSERT INTO current_assessment_frame(record_id, recorded_at, eeg_index, emg_index, delta_rel_1, delta_rel_2, theta_rel_1, theta_rel_2, alpha_rel_1, alpha_rel_2, beta_rel_1, beta_rel_2, power_1, power_2, median_1, median_2, edge_freq_1, edge_freq_2, artifacts_1, artifacts_2, alerts , info, bsr_short_1, bsr_medium_1, reserved_1, sti_ch_1, sti_ch_2, bsr_short_2, bsr_medium_2, ibi_ch_1, ibi_ch_2, a_eeg_min_1, a_eeg_max_1, a_eeg_min_2, a_eeg_max_2, reserved_2, chk_sum, raw) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", false);
        artifacts1 = new byte[2];
        artifacts2 = new byte[2];
        info = new byte[2];
        reserved1 = new byte[2];
        reserved2 = new byte[4];
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
            statement.setShort(3, buffer.getShort()); // eeg_index
            statement.setShort(4, buffer.getShort()); // emg_index
            statement.setFloat(5, buffer.getFloat()); // delta_rel_1
            statement.setFloat(6, buffer.getFloat()); // delta_rel_2
            statement.setFloat(7, buffer.getFloat()); // theta_rel_1
            statement.setFloat(8, buffer.getFloat()); // theta_rel_2
            statement.setFloat(9, buffer.getFloat()); // alpha_rel_1
            statement.setFloat(10, buffer.getFloat()); // alpha_rel_2
            statement.setFloat(11, buffer.getFloat()); // beta_rel_1
            statement.setFloat(12, buffer.getFloat()); // beta_rel_2
            statement.setFloat(13, buffer.getFloat()); // power_1
            statement.setFloat(14, buffer.getFloat()); // power_2
            statement.setFloat(15, buffer.getFloat()); // median_1
            statement.setFloat(16, buffer.getFloat()); // median_2
            statement.setFloat(17, buffer.getFloat()); // edge_freq_1
            statement.setFloat(18, buffer.getFloat()); // edge_freq_2
            buffer.get(artifacts1); // artifacts_1
            statement.setBytes(19, artifacts1); // artifacts_1
            buffer.get(artifacts2); // artifacts_2
            statement.setBytes(20, artifacts2); // artifacts_2
            statement.setByte(21, buffer.get()); // alerts
            buffer.get(info);
            statement.setBytes(22, info); // info
            statement.setFloat(23, buffer.getFloat()); // bsr_short_1
            statement.setFloat(24, buffer.getFloat()); // bsr_medium_1
            buffer.get(reserved1); // reserved_1
            statement.setBytes(25, reserved1); // reserved_1
            statement.setShort(26, buffer.get()); // sti_ch_1
            statement.setShort(27, buffer.get()); // sti_ch_2
            statement.setFloat(28, buffer.getFloat()); // bsr_short_2
            statement.setFloat(29, buffer.getFloat()); // bsr_medium_2
            statement.setShort(30, buffer.getShort()); // ibi_ch_1
            statement.setShort(31, buffer.getShort()); // ibi_ch_2
            statement.setFloat(32, buffer.getFloat()); // a_eeg_min_1
            statement.setFloat(33, buffer.getFloat()); // a_eeg_max_1
            statement.setFloat(34, buffer.getFloat()); // a_eeg_min_2
            statement.setFloat(35, buffer.getFloat()); // a_eeg_max_2
            buffer.get(reserved2); // reserved_2
            statement.setBytes(36, reserved2); // reserved_2
            buffer.get(chkSum); // chk_sum
            statement.setBytes(37, chkSum); // chk_sum
            statement.setBytes(38, raw); // raw
            statement.addBatch();
            batchCounter++;
            logger.debug("Created Current Assessment batch");

            if(batchCounter >= 0) {
                statement.executeBatch();
                batchCounter = 0;
                logger.debug("Send batches");
            }
        } catch (Exception e) {
            logger.error("Error processing Current Assessment data, maybe timeDifference: long cannot be cast to int", e);
        } finally {
            buffer.get();
            handleRemains(buffer);
        }

    }
}
