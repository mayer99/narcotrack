package com.mayer.listeners;

import com.mayer.Narcotrack;
import com.mayer.NarcotrackEventHandler;
import com.mayer.events.CurrentAssessmentEvent;
import com.mayer.events.EEGEvent;
import com.mayer.events.ElectrodeCheckEvent;
import com.mayer.events.PowerSpectrumEvent;
import com.mayer.events.RemainsEvent;
import com.mayer.frames.CurrentAssessment;
import com.mayer.frames.ElectrodeCheck;
import com.mayer.frames.PowerSpectrum;
import com.mayer.Remains;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;
import java.util.Arrays;

public class MariaDatabaseHandler implements NarcotrackEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MariaDatabaseHandler.class);

    private final String NARCOTRACK_DB_URL = System.getenv("NARCOTRACK_DB_URL");
    private final String NARCOTRACK_DB_TABLE = System.getenv("NARCOTRACK_DB_TABLE");
    private final String NARCOTRACK_DB_USERNAME = System.getenv("NARCOTRACK_DB_USERNAME");
    private final String NARCOTRACK_DB_PASSWORD = System.getenv("NARCOTRACK_DB_PASSWORD");
    private final Instant startTime;
    private Connection databaseConnection;
    private int recordId;
    private PreparedStatement recordingsStatement, eegStatement, currentAssessmentStatement, powerSpectrumStatement, spectrumStatement, electrodeCheckStatement, remainsStatement;
    private int eegBatchCounter = 0, currentAssessmentBatchCounter = 0;
    private final int eegBatchMax = 32, currentAssessmentBatchMax = 2;

    public MariaDatabaseHandler(Narcotrack narcotrack) {
        startTime = narcotrack.getStartTime();

        try {
            LOGGER.debug("Connecting to database {}", NARCOTRACK_DB_URL);
            databaseConnection = DriverManager.getConnection("jdbc:mariadb://" + NARCOTRACK_DB_URL + ":3306/" + NARCOTRACK_DB_TABLE, NARCOTRACK_DB_USERNAME, NARCOTRACK_DB_PASSWORD);
            Runtime.getRuntime().addShutdownHook(new DatabaseShutdownHook());

            recordingsStatement = databaseConnection.prepareStatement("INSERT INTO recordings(start_time) VALUES(?)", Statement.RETURN_GENERATED_KEYS);
            eegStatement = databaseConnection.prepareStatement("INSERT INTO eeg_frame(record_id, recorded_at, frameCountPerSecond, raw) VALUES(?, ?, ?, ?)");
            currentAssessmentStatement = databaseConnection.prepareStatement("INSERT INTO current_assessment_frame(record_id, recorded_at, frameCountPerSecond, eeg_index, emg_index, delta_rel_1, delta_rel_2, theta_rel_1, theta_rel_2, alpha_rel_1, alpha_rel_2, beta_rel_1, beta_rel_2, power_1, power_2, median_1, median_2, edge_freq_1, edge_freq_2, artifacts_1, artifacts_2, alerts , info, bsr_short_1, bsr_medium_1, reserved_1, sti_ch_1, sti_ch_2, bsr_short_2, bsr_medium_2, ibi_ch_1, ibi_ch_2, a_eeg_min_1, a_eeg_max_1, a_eeg_min_2, a_eeg_max_2, reserved_2, chk_sum, raw) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            powerSpectrumStatement = databaseConnection.prepareStatement("INSERT INTO power_spectrum_frame(record_id, recorded_at, frameCountPerSecond, info, chk_sum, raw) VALUES(?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            spectrumStatement = databaseConnection.prepareStatement("INSERT INTO power_spectrum(frame_id, channel, 0_0, 0_5, 1_0, 1_5, 2_0, 2_5, 3_0, 3_5, 4_0, 4_5, 5_0, 5_5, 6_0, 6_5, 7_0, 7_5, 8_0, 8_5, 9_0, 9_5, 10_0, 10_5, 11_0, 11_5, 12_0, 12_5, 13_0, 13_5, 14_0, 14_5, 15_0, 15_5, 16_0, 16_5, 17_0, 17_5, 18_0, 18_5, 19_0, 19_5, 20_0, 20_5, 21_0, 21_5, 22_0, 22_5, 23_0, 23_5, 24_0, 24_5, 25_0, 25_5, 26_0, 26_5, 27_0, 27_5, 28_0, 28_5, 29_0, 29_5, 30_0, 30_5, 31_0, 31_5, 32_0, 32_5, 33_0, 33_5, 34_0, 34_5, 35_0, 35_5, 36_0, 36_5, 37_0, 37_5, 38_0, 38_5, 39_0, 39_5, 40_0, 40_5, 41_0, 41_5, 42_0, 42_5, 43_0, 43_5, 44_0, 44_5, 45_0, 45_5, 46_0, 46_5, 47_0, 47_5, 48_0, 48_5, 49_0, 49_5, 50_0, 50_5, 51_0, 51_5, 52_0, 52_5, 53_0, 53_5, 54_0, 54_5, 55_0, 55_5, 56_0, 56_5, 57_0, 57_5, 58_0, 58_5, 59_0, 59_5, 60_0, 60_5, 61_0, 61_5, 62_0, 62_5, 63_0, 63_5) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            electrodeCheckStatement = databaseConnection.prepareStatement("INSERT INTO electrode_check_frame(record_id, recorded_at, frameCountPerSecond, imp_1_a, imp_1_b, imp_ref, imp_2_a, imp_2_b, info, chk_sum, raw) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            remainsStatement = databaseConnection.prepareStatement("INSERT INTO remains(record_id, recorded_at, raw) VALUES(?, ?, ?)");

            createRecording();
        } catch (Exception e) {
            LOGGER.error("Error creating connection, creating prepared statements or insert recording, Exception Message: {}", e.getMessage(), e);
            Narcotrack.rebootPlatform();
        }

        EEGEvent.getEventHandlers().add(this);
        CurrentAssessmentEvent.getEventHandlers().add(this);
        PowerSpectrumEvent.getEventHandlers().add(this);
        ElectrodeCheckEvent.getEventHandlers().add(this);
        RemainsEvent.getEventHandlers().add(this);
    }

    class DatabaseShutdownHook extends Thread {
        public void run() {
            LOGGER.warn("Shutting down database");
            try {
                if (databaseConnection != null && !databaseConnection.isClosed()) {
                    databaseConnection.close();
                    LOGGER.info("Closed DB connection");
                }
            } catch (SQLException e) {
                LOGGER.error("Error closing database connection, Exception Message: {}", e.getMessage(), e);
            }
        }
    }

    public void createRecording() throws SQLException {
        recordingsStatement.setTimestamp(1, Timestamp.from(startTime));
        if(recordingsStatement.executeUpdate() < 1) {
            LOGGER.error("Could not insert recording, got no rows back");
            Narcotrack.rebootPlatform();
        }
        ResultSet rs = recordingsStatement.getGeneratedKeys();
        if(!rs.next()) {
            LOGGER.error("Insert of recording did not return an id key");
            Narcotrack.rebootPlatform();
        }
        recordId = rs.getInt(1);
        // recordId = rs.getInt("insert_id");
        rs.close();
        LOGGER.info("Created recording, id is {}", recordId);
    }


    @Override
    public void onEEGEvent(EEGEvent event) {

        try {
            eegStatement.setInt(1, recordId);
            eegStatement.setInt(2, event.getTime());
            eegStatement.setInt(3, event.getFrameCount());
            eegStatement.setBytes(4, event.getData().getRaw());
            eegStatement.addBatch();
            LOGGER.debug("Created EEG batch");
            eegBatchCounter++;

            if (eegBatchCounter >= eegBatchMax) {
                eegStatement.executeBatch();
                eegBatchCounter = 0;
                LOGGER.debug("Sent EEG batch");
            }
        } catch (SQLException e) {
            LOGGER.error("Error processing EEG data, Exception Message: {}", e.getMessage(), e);
            try {
                eegStatement.clearBatch();
                eegBatchCounter = 0;
            } catch (SQLException ex) {
                LOGGER.error("Could not clear EEG Batch, Exception Message: {}", ex.getMessage(), ex);
            }
        }

    }

    @Override
    public void onCurrentAssessmentEvent(CurrentAssessmentEvent event) {

        try {
            final CurrentAssessment data = event.getData();
            currentAssessmentStatement.setInt(1, recordId); // record_id
            currentAssessmentStatement.setInt(2, event.getTime()); // recorded_at
            currentAssessmentStatement.setInt(3, event.getFrameCount()); // recorded_at
            currentAssessmentStatement.setShort(4, data.getEegIndex()); // eeg_index
            currentAssessmentStatement.setShort(5, data.getEmgIndex()); // emg_index
            currentAssessmentStatement.setFloat(6, data.getDeltaRel1()); // delta_rel_1
            currentAssessmentStatement.setFloat(7, data.getDeltaRel2()); // delta_rel_2
            currentAssessmentStatement.setFloat(8, data.getThetaRel1()); // theta_rel_1
            currentAssessmentStatement.setFloat(9, data.getThetaRel2()); // theta_rel_2
            currentAssessmentStatement.setFloat(10, data.getAlphaRel1()); // alpha_rel_1
            currentAssessmentStatement.setFloat(11, data.getAlphaRel2()); // alpha_rel_2
            currentAssessmentStatement.setFloat(12, data.getBetaRel1()); // beta_rel_1
            currentAssessmentStatement.setFloat(13, data.getBetaRel2()); // beta_rel_2
            currentAssessmentStatement.setFloat(14, data.getPower1()); // power_1
            currentAssessmentStatement.setFloat(15, data.getPower2()); // power_2
            currentAssessmentStatement.setFloat(16, data.getMedian1()); // median_1
            currentAssessmentStatement.setFloat(17, data.getMedian2()); // median_2
            currentAssessmentStatement.setFloat(18, data.getEdgeFreq1()); // edge_freq_1
            currentAssessmentStatement.setFloat(19, data.getEdgeFreq2()); // edge_freq_2
            currentAssessmentStatement.setBytes(20, data.getArtifacts1()); // artifacts_1
            currentAssessmentStatement.setBytes(21, data.getArtifacts2()); // artifacts_2
            currentAssessmentStatement.setByte(22, data.getAlerts()); // alerts
            currentAssessmentStatement.setBytes(23, data.getInfo()); // info
            currentAssessmentStatement.setFloat(24, data.getBsrShort1()); // bsr_short_1
            currentAssessmentStatement.setFloat(25, data.getBsrMedium1()); // bsr_medium_1
            currentAssessmentStatement.setBytes(26, data.getReserved1()); // reserved_1
            currentAssessmentStatement.setShort(27, data.getStiCh1()); // sti_ch_1
            currentAssessmentStatement.setShort(28, data.getStiCh2()); // sti_ch_2
            currentAssessmentStatement.setFloat(29, data.getBsrShort2()); // bsr_short_2
            currentAssessmentStatement.setFloat(30, data.getBsrMedium2()); // bsr_medium_2
            currentAssessmentStatement.setShort(31, data.getIbiCh1()); // ibi_ch_1
            currentAssessmentStatement.setShort(32, data.getIbiCh2()); // ibi_ch_2
            currentAssessmentStatement.setFloat(33, data.getaEEGMin1()); // a_eeg_min_1
            currentAssessmentStatement.setFloat(34, data.getaEEGMax1()); // a_eeg_max_1
            currentAssessmentStatement.setFloat(35, data.getaEEGMin2()); // a_eeg_min_2
            currentAssessmentStatement.setFloat(36, data.getaEEGMax2()); // a_eeg_max_2
            currentAssessmentStatement.setBytes(37, data.getReserved2()); // reserved_2
            currentAssessmentStatement.setBytes(38, data.getChkSum()); // chk_sum
            currentAssessmentStatement.setBytes(39, data.getRaw()); // raw
            currentAssessmentStatement.addBatch();
            LOGGER.debug("Created Current Assessment batch");
            currentAssessmentBatchCounter++;

            if(currentAssessmentBatchCounter >= currentAssessmentBatchMax) {
                currentAssessmentStatement.executeBatch();
                currentAssessmentBatchCounter = 0;
                LOGGER.debug("Sent Current Assessment batch");
            }
        } catch (SQLException e) {
            LOGGER.error("Error processing Current Assessment data, Exception Message: {}", e.getMessage(), e);
            try {
                currentAssessmentStatement.clearBatch();
                currentAssessmentBatchCounter = 0;
            } catch (SQLException ex) {
                LOGGER.error("Could not clear currentAssessment Batch, Exception Message: {}", ex.getMessage(), ex);
            }
        }
    }

    @Override
    public void onPowerSpectrumEvent(PowerSpectrumEvent event) {

        try {
            final PowerSpectrum data = event.getData();
            powerSpectrumStatement.setInt(1, recordId);
            powerSpectrumStatement.setInt(2, event.getTime());
            powerSpectrumStatement.setInt(3, event.getFrameCount());
            powerSpectrumStatement.setByte(4, data.getInfo());
            powerSpectrumStatement.setBytes(5, data.getChkSum());
            powerSpectrumStatement.setBytes(6, data.getRaw());
            if (powerSpectrumStatement.executeUpdate() > 0) {
                ResultSet rs = powerSpectrumStatement.getGeneratedKeys();
                if(rs.next()) {
                    final int frameId = rs.getInt(1);
                    insertSpectrum(frameId, 1, data.getSpectrum1());
                    insertSpectrum(frameId, 2, data.getSpectrum2());
                } else {
                    LOGGER.error("Error processing power spectrum block. Result has keys, but resultset cannot be moved to next()");
                }
                rs.close();
            } else {
                LOGGER.error("Error sending block for power spectrum. Did not receive keys as result");
            }
            LOGGER.debug("Sent Power Spectrum");
        } catch (SQLException e) {
            LOGGER.error("Error processing Power Spectrum data, Exception Message: {}", e.getMessage(), e);
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

    @Override
    public void onElectrodeCheckEvent(ElectrodeCheckEvent event) {

        try {
            final ElectrodeCheck data = event.getData();
            electrodeCheckStatement.setInt(1, recordId);
            electrodeCheckStatement.setInt(2, event.getTime());
            electrodeCheckStatement.setInt(3, event.getFrameCount());
            electrodeCheckStatement.setFloat(4, adjustImpedance(data.getImp1a()));
            electrodeCheckStatement.setFloat(5, adjustImpedance(data.getImp1b()));
            electrodeCheckStatement.setFloat(6, adjustImpedance(data.getImpRef()));
            electrodeCheckStatement.setFloat(7, adjustImpedance(data.getImp2a()));
            electrodeCheckStatement.setFloat(8, adjustImpedance(data.getImp2b()));
            electrodeCheckStatement.setByte(9, data.getInfo());
            electrodeCheckStatement.setBytes(10, data.getChkSum());
            electrodeCheckStatement.setBytes(11, data.getRaw());
            electrodeCheckStatement.executeUpdate();
            LOGGER.debug("Sent Electrode Check");
        } catch (SQLException e) {
            LOGGER.error("Error processing Electrode Check data, Exception Message: {}", e.getMessage(), e);
        }
    }

    private float adjustImpedance(float impedance) {
        if (Float.isInfinite(impedance) || Float.isNaN(impedance)) {
            LOGGER.warn("Received impedance that is infinite or NaN");
            return -1;
        }
        return impedance;
    }

    @Override
    public void onRemainsEvent(RemainsEvent event) {

        try {
            final Remains data = event.getData();
            remainsStatement.setInt(1, recordId);
            remainsStatement.setInt(2, event.getTime());
            if (data.getChunks().size() > 1) {
                for (byte[] chunk: data.getChunks()) {
                    remainsStatement.setBytes(3, chunk);
                    remainsStatement.addBatch();
                }
                remainsStatement.executeBatch();
                LOGGER.debug("Sent Remains batch");
            } else {
                remainsStatement.setBytes(3, data.getChunks().get(0));
                remainsStatement.executeUpdate();
                LOGGER.debug("Sent Remains");
            }
        } catch (SQLException e) {
            LOGGER.error("Error processing Remains data, Exception Message: {}", e.getMessage(), e);
        }
    }
}
