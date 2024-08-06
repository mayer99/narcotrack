package com.mayer99.narcotrack.handlers;

import com.mayer99.Narcotrack;
import com.mayer99.narcotrack.base.events.CurrentAssessmentEvent;
import com.mayer99.narcotrack.base.events.EEGEvent;
import com.mayer99.narcotrack.base.events.ElectrodeCheckEvent;
import com.mayer99.narcotrack.base.events.PowerSpectrumEvent;
import com.mayer99.narcotrack.base.events.RemainsEvent;
import com.mayer99.narcotrack.base.frames.*;
import com.mayer99.narcotrack.base.models.NarcotrackEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;
import java.util.Arrays;

public class MariaDatabaseHandler implements NarcotrackEventHandler {

    private static final int MAX_INTERVALS = 10;
    private static final String DB_URL = "NT_DB_URL";
    private static final String DB_USERNAME = "NT_DB_USERNAME";
    private static final String DB_PASSWORD = "NT_DB_PASSWORD";

    private static final Logger logger = LoggerFactory.getLogger(MariaDatabaseHandler.class);

    private final Connection dbConnection;
    private final PreparedStatement recordingStatement, eegStatement, currentAssessmentStatement, powerSpectrumStatement, spectrumStatement, electrodeCheckStatement, remainsStatement;

    private short recordId;
    private short eegCounter = 0;
    private int intervalCounter = 0;
    private boolean hasEEGs = false;
    private boolean hasCurrentAssessments = false;


    public MariaDatabaseHandler() throws Exception {
        logger.info("MariaDatabaseHandler starting...");

        String dbUrl = System.getenv(DB_URL);
        if (Narcotrack.isNullEmptyOrWhitespace(dbUrl)) {
            throw new IllegalArgumentException(String.format("The environment variable %s ist not set or has an invalid value. Are the environment variables loaded?", DB_URL));
        }

        String dbUsername = System.getenv(DB_USERNAME);
        if (Narcotrack.isNullEmptyOrWhitespace(dbUsername)) {
            throw new IllegalArgumentException(String.format("The environment variable %s ist not set or has an invalid value. Are the environment variables loaded?", DB_USERNAME));
        }

        String dbPassword = System.getenv(DB_PASSWORD);
        if (Narcotrack.isNullEmptyOrWhitespace(dbPassword)) {
            throw new IllegalArgumentException(String.format("The environment variable %s ist not set or has an invalid value. Are the environment variables loaded?", DB_PASSWORD));
        }

        logger.info("Connecting to database {} as {}", dbUrl, dbUsername);
        dbConnection = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
        Runtime.getRuntime().addShutdownHook(new DatabaseShutdownHook());

        recordingStatement = dbConnection.prepareStatement("INSERT INTO recordings(start_time) VALUES(?)", Statement.RETURN_GENERATED_KEYS);
        eegStatement = dbConnection.prepareStatement("INSERT INTO frame_eeg(record_id, recorded_at, frameCountPerSecond, chk_sum, checksum_valid, raw) VALUES(?, ?, ?, ?, ?, ?)");
        currentAssessmentStatement = dbConnection.prepareStatement("INSERT INTO frame_current_assessment(record_id, recorded_at, eeg_index, emg_index, delta_rel_1, delta_rel_2, theta_rel_1, theta_rel_2, alpha_rel_1, alpha_rel_2, beta_rel_1, beta_rel_2, power_1, power_2, median_1, median_2, edge_freq_1, edge_freq_2, artifacts_1, artifacts_2, alerts , info, bsr_short_1, bsr_medium_1, reserved_1, sti_ch_1, sti_ch_2, bsr_short_2, bsr_medium_2, ibi_ch_1, ibi_ch_2, a_eeg_min_1, a_eeg_max_1, a_eeg_min_2, a_eeg_max_2, reserved_2, chk_sum, checksum_valid, raw) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        powerSpectrumStatement = dbConnection.prepareStatement("INSERT INTO frame_power_spectrum(record_id, recorded_at, info, chk_sum, checksum_valid, raw) VALUES(?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
        spectrumStatement = dbConnection.prepareStatement("INSERT INTO power_spectrum(frame_id, channel, 0_0, 0_5, 1_0, 1_5, 2_0, 2_5, 3_0, 3_5, 4_0, 4_5, 5_0, 5_5, 6_0, 6_5, 7_0, 7_5, 8_0, 8_5, 9_0, 9_5, 10_0, 10_5, 11_0, 11_5, 12_0, 12_5, 13_0, 13_5, 14_0, 14_5, 15_0, 15_5, 16_0, 16_5, 17_0, 17_5, 18_0, 18_5, 19_0, 19_5, 20_0, 20_5, 21_0, 21_5, 22_0, 22_5, 23_0, 23_5, 24_0, 24_5, 25_0, 25_5, 26_0, 26_5, 27_0, 27_5, 28_0, 28_5, 29_0, 29_5, 30_0, 30_5, 31_0, 31_5, 32_0, 32_5, 33_0, 33_5, 34_0, 34_5, 35_0, 35_5, 36_0, 36_5, 37_0, 37_5, 38_0, 38_5, 39_0, 39_5, 40_0, 40_5, 41_0, 41_5, 42_0, 42_5, 43_0, 43_5, 44_0, 44_5, 45_0, 45_5, 46_0, 46_5, 47_0, 47_5, 48_0, 48_5, 49_0, 49_5, 50_0, 50_5, 51_0, 51_5, 52_0, 52_5, 53_0, 53_5, 54_0, 54_5, 55_0, 55_5, 56_0, 56_5, 57_0, 57_5, 58_0, 58_5, 59_0, 59_5, 60_0, 60_5, 61_0, 61_5, 62_0, 62_5, 63_0, 63_5) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        electrodeCheckStatement = dbConnection.prepareStatement("INSERT INTO frame_electrode_check(record_id, recorded_at, imp_1_a, imp_1_b, imp_ref, imp_2_a, imp_2_b, info, chk_sum, checksum_valid, raw) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        remainsStatement = dbConnection.prepareStatement("INSERT INTO remains(record_id, recorded_at, raw) VALUES(?, ?, ?)");
    }

    class DatabaseShutdownHook extends Thread {
        public void run() {
            logger.warn("DatabaseShutdownHook triggered");
            try {
                if (dbConnection == null || dbConnection.isClosed()) {
                    logger.info("DatabaseConnection is null or closed");
                    return;
                }
                logger.info("Attempting to close DatabaseConnection");
                dbConnection.close();
                logger.info("Closed DB connection");
            } catch (Exception e) {
                logger.error("Could not close DatabaseConnection", e);
            }
        }
    }

    @Override
    public void onRecordingStart(Instant instant) {
        try {
            recordingStatement.setTimestamp(1, Timestamp.from(instant));
            if(recordingStatement.executeUpdate() < 1) {
                throw new Exception("Could not insert recording, got no rows back");
            }
            ResultSet rs = recordingStatement.getGeneratedKeys();
            if(!rs.next()) {
                throw new Exception("Insert of recording did not return an id key");
            }
            recordId = rs.getShort(1);
            rs.close();
            logger.info("Created recording, id is {}", recordId);
            eegCounter = 0;
            hasEEGs = false;
            hasCurrentAssessments = false;
            intervalCounter = 0;
        } catch (Exception e) {
            logger.error("Could not start new recording", e);
            Narcotrack.rebootPlatform();
        }
    }

    @Override
    public void onRecordingStop() {
        logger.debug("Entering1");
        recordId = -1;
        logger.info("Removed recordId");
        try {
            if (hasEEGs) {
                eegStatement.executeBatch();
                logger.debug("Sent EEG batch");
            }
            if (hasCurrentAssessments) {
                currentAssessmentStatement.executeBatch();
                logger.debug("Sent CurrentAssessment batch");
            }
        } catch (SQLException e) {
            logger.error("Error processing EEG or CurrentAssessment Frame", e);
            try {
                eegStatement.clearBatch();
                currentAssessmentStatement.clearBatch();
            } catch (SQLException ex) {
                logger.error("Could not clear batches, attempting restart", e);
                Narcotrack.rebootPlatform();
            }
        } finally {
            hasEEGs = false;
            hasCurrentAssessments = false;
            intervalCounter = 0;
        }
    }

    @Override
    public void onEEG(EEGEvent event) {
        if (recordId < 0) {
            logger.error("Could not process EEGEvent, there is no recording");
            return;
        }
        try {
            EEG data = event.getData();
            eegStatement.setShort(1, recordId);
            eegStatement.setLong(2, event.getTime());
            eegStatement.setShort(3, eegCounter);
            eegStatement.setBytes(4, data.getChkSum());
            eegStatement.setBoolean(5, data.isChecksumValid());
            eegStatement.setBytes(6, data.getRaw());
            eegStatement.addBatch();
            if (!hasEEGs) hasEEGs = true;
            eegCounter++;
        } catch (SQLException e) {
            logger.error("Error processing EEG data", e);
            try {
                eegStatement.clearBatch();
                logger.info("Cleared eegBatch");
            } catch (SQLException ex) {
                logger.error("Could not clear EEG Batch", ex);
            } finally {
                eegCounter = 0;
                hasEEGs = false;
            }
        }
    }

    @Override
    public void onCurrentAssessment(CurrentAssessmentEvent event) {
        if (recordId < 0) {
            logger.error("Could not process CurrentAssessmentEvent, there is no recording");
            return;
        }
        try {
            CurrentAssessment data = event.getData();
            currentAssessmentStatement.setShort(1, recordId); // record_id
            currentAssessmentStatement.setLong(2, event.getTime()); // recorded_at
            currentAssessmentStatement.setShort(3, data.getEegIndex()); // eeg_index
            currentAssessmentStatement.setShort(4, data.getEmgIndex()); // emg_index
            currentAssessmentStatement.setFloat(5, data.getDeltaRel1()); // delta_rel_1
            currentAssessmentStatement.setFloat(6, data.getDeltaRel2()); // delta_rel_2
            currentAssessmentStatement.setFloat(7, data.getThetaRel1()); // theta_rel_1
            currentAssessmentStatement.setFloat(8, data.getThetaRel2()); // theta_rel_2
            currentAssessmentStatement.setFloat(9, data.getAlphaRel1()); // alpha_rel_1
            currentAssessmentStatement.setFloat(10, data.getAlphaRel2()); // alpha_rel_2
            currentAssessmentStatement.setFloat(11, data.getBetaRel1()); // beta_rel_1
            currentAssessmentStatement.setFloat(12, data.getBetaRel2()); // beta_rel_2
            currentAssessmentStatement.setFloat(13, data.getPower1()); // power_1
            currentAssessmentStatement.setFloat(14, data.getPower2()); // power_2
            currentAssessmentStatement.setFloat(15, data.getMedian1()); // median_1
            currentAssessmentStatement.setFloat(16, data.getMedian2()); // median_2
            currentAssessmentStatement.setFloat(17, data.getEdgeFreq1()); // edge_freq_1
            currentAssessmentStatement.setFloat(18, data.getEdgeFreq2()); // edge_freq_2
            currentAssessmentStatement.setBytes(19, data.getArtifacts1()); // artifacts_1
            currentAssessmentStatement.setBytes(20, data.getArtifacts2()); // artifacts_2
            currentAssessmentStatement.setByte(21, data.getAlerts()); // alerts
            currentAssessmentStatement.setBytes(22, data.getInfo()); // info
            currentAssessmentStatement.setFloat(23, data.getBsrShort1()); // bsr_short_1
            currentAssessmentStatement.setFloat(24, data.getBsrMedium1()); // bsr_medium_1
            currentAssessmentStatement.setBytes(25, data.getReserved1()); // reserved_1
            currentAssessmentStatement.setByte(26, data.getStiCh1()); // sti_ch_1
            currentAssessmentStatement.setByte(27, data.getStiCh2()); // sti_ch_2
            currentAssessmentStatement.setFloat(28, data.getBsrShort2()); // bsr_short_2
            currentAssessmentStatement.setFloat(29, data.getBsrMedium2()); // bsr_medium_2
            currentAssessmentStatement.setShort(30, data.getIbiCh1()); // ibi_ch_1
            currentAssessmentStatement.setShort(31, data.getIbiCh2()); // ibi_ch_2
            currentAssessmentStatement.setFloat(32, data.getaEEGMin1()); // a_eeg_min_1
            currentAssessmentStatement.setFloat(33, data.getaEEGMax1()); // a_eeg_max_1
            currentAssessmentStatement.setFloat(34, data.getaEEGMin2()); // a_eeg_min_2
            currentAssessmentStatement.setFloat(35, data.getaEEGMax2()); // a_eeg_max_2
            currentAssessmentStatement.setBytes(36, data.getReserved2()); // reserved_2
            currentAssessmentStatement.setBytes(37, data.getChkSum()); // chk_sum
            currentAssessmentStatement.setBoolean(38, data.isChecksumValid());
            currentAssessmentStatement.setBytes(39, data.getRaw()); // raw
            currentAssessmentStatement.addBatch();
            if (!hasCurrentAssessments) hasCurrentAssessments = true;
        } catch (SQLException e) {
            logger.error("Error processing Current Assessment data", e);
            try {
                currentAssessmentStatement.clearBatch();
            } catch (SQLException ex) {
                logger.error("Could not clear currentAssessment Batch", ex);
            } finally {
                hasCurrentAssessments = false;
            }
        }
    }

    @Override
    public void onPowerSpectrum(PowerSpectrumEvent event) {
        if (recordId < 0) {
            logger.error("Could not process PowerSpectrumEvent, there is no recording");
            return;
        }
        try {
            PowerSpectrum data = event.getData();
            powerSpectrumStatement.setShort(1, recordId);
            powerSpectrumStatement.setLong(2, event.getTime());
            powerSpectrumStatement.setByte(3, data.getInfo());
            powerSpectrumStatement.setBytes(4, data.getChkSum());
            powerSpectrumStatement.setBoolean(5, data.isChecksumValid());
            powerSpectrumStatement.setBytes(6, data.getRaw());
            if (powerSpectrumStatement.executeUpdate() < 1) {
                logger.error("Error sending block for power spectrum. Did not receive keys as result");
                return;
            }
            ResultSet rs = powerSpectrumStatement.getGeneratedKeys();
            if(rs.next()) {
                int frameId = rs.getInt(1);
                insertSpectrum(frameId, 1, data.getSpectrum1());
                insertSpectrum(frameId, 2, data.getSpectrum2());
            } else {
                logger.error("Error processing power spectrum block. Result has keys, but ResultSet is empty");
            }
            rs.close();
        } catch (SQLException e) {
            logger.error("Error processing Power Spectrum data", e);
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
    public void onElectrodeCheck(ElectrodeCheckEvent event) {
        if (recordId < 0) {
            logger.error("Could not process ElectrodeCheckEvent, there is no recording");
            return;
        }
        try {
            ElectrodeCheck data = event.getData();
            electrodeCheckStatement.setShort(1, recordId);
            electrodeCheckStatement.setLong(2, event.getTime());
            electrodeCheckStatement.setFloat(3, adjustImpedance(data.getImp1a()));
            electrodeCheckStatement.setFloat(4, adjustImpedance(data.getImp1b()));
            electrodeCheckStatement.setFloat(5, adjustImpedance(data.getImpRef()));
            electrodeCheckStatement.setFloat(6, adjustImpedance(data.getImp2a()));
            electrodeCheckStatement.setFloat(7, adjustImpedance(data.getImp2b()));
            electrodeCheckStatement.setByte(8, data.getInfo());
            electrodeCheckStatement.setBytes(9, data.getChkSum());
            electrodeCheckStatement.setBoolean(10, data.isChecksumValid());
            electrodeCheckStatement.setBytes(11, data.getRaw());
            electrodeCheckStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error processing Electrode Check data", e);
        }
    }

    private float adjustImpedance(float impedance) {
        if (Float.isInfinite(impedance) || Float.isNaN(impedance)) {
            logger.warn("Received impedance that is infinite or NaN");
            return -1;
        }
        return impedance;
    }

    @Override
    public void onRemains(RemainsEvent event) {
        if (recordId < 0) {
            logger.error("Could not process RemainsEvent, there is no recording");
            return;
        }
        try {
            Remains data = event.getData();
            remainsStatement.setShort(1, recordId);
            remainsStatement.setLong(2, event.getTime());
            for (byte[] chunk: data.getChunks()) {
                remainsStatement.setBytes(3, chunk);
                remainsStatement.addBatch();
            }
            remainsStatement.executeBatch();
            logger.warn("Sent Remains batch");
        } catch (SQLException e) {
            logger.error("Error processing Remains data", e);
        }
    }

    @Override
    public void onEndOfInterval() {
        eegCounter = 0;
        intervalCounter++;
        if (intervalCounter >= MAX_INTERVALS) {
            intervalCounter = 0;
            try {
                if (hasEEGs) {
                    eegStatement.executeBatch();
                    logger.debug("Sent EEG batch");
                }
                if (hasCurrentAssessments) {
                    currentAssessmentStatement.executeBatch();
                    logger.debug("Sent CurrentAssessment batch");
                }
            } catch (SQLException e) {
                logger.error("Error processing EEG or CurrentAssessment Frame", e);
                try {
                    eegStatement.clearBatch();
                    currentAssessmentStatement.clearBatch();
                } catch (SQLException ex) {
                    logger.error("Could not clear batches, attempting restart", e);
                    Narcotrack.rebootPlatform();
                }
            } finally {
                hasEEGs = false;
                hasCurrentAssessments = false;
            }
        }
    }
}
