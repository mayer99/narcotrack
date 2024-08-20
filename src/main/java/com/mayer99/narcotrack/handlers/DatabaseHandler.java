package com.mayer99.narcotrack.handlers;

import com.mayer99.narcotrack.application.NarcotrackApplication;
import com.mayer99.narcotrack.NarcotrackEventHandler;
import com.mayer99.narcotrack.NarcotrackEventManager;
import com.mayer99.narcotrack.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;
import java.util.Arrays;

public class DatabaseHandler implements NarcotrackEventHandler {

    private final Logger LOGGER = LoggerFactory.getLogger(DatabaseHandler.class);
    private final NarcotrackApplication application;
    private final NarcotrackEventManager eventManager;

    private Connection dbConnection;
    private PreparedStatement recordingStatement, eegStatement, currentAssessmentStatement, powerSpectrumStatement, spectrumStatement, electrodeCheckStatement, remainsStatement;

    private boolean isRecording = false;
    private short recordId;
    private short eegCounter = 0;
    private int intervalCounter = 0;
    private boolean hasEEGs = false;
    private boolean hasCurrentAssessments = false;


    public DatabaseHandler(NarcotrackApplication application) {
        LOGGER.info("DatabaseHandler starting...");
        this.application = application;
        eventManager = application.getEventManager();

        String databaseURL = application.getConfig("DATABASE_URL");
        String databaseUsername = application.getConfig("DATABASE_USERNAME");
        String databasePassword = application.getConfig("DATABASE_PASSWORD");

        if (databaseURL == null || databaseURL.trim().isEmpty()) {
            LOGGER.error("DATABASE_URL is null, empty or whitespace. Please check the configuration file");
            eventManager.dispatchOnUnrecoverableError();
            System.exit(1);
        }

        if (databaseUsername == null || databaseUsername.trim().isEmpty()) {
            LOGGER.error("DATABASE_USERNAME is null, empty or whitespace. Please check the configuration file");
            eventManager.dispatchOnUnrecoverableError();
            System.exit(1);
        }

        if (databasePassword == null || databasePassword.trim().isEmpty()) {
            LOGGER.error("DATABASE_PASSWORD is null, empty or whitespace. Please check the configuration file");
            eventManager.dispatchOnUnrecoverableError();
            System.exit(1);
        }

        LOGGER.info("Connecting to database {} as {}", databaseURL, databaseUsername);
        try {
            dbConnection = DriverManager.getConnection(databaseURL, databaseUsername, databasePassword);
            recordingStatement = dbConnection.prepareStatement("INSERT INTO recordings(start_time) VALUES(?)", Statement.RETURN_GENERATED_KEYS);
            eegStatement = dbConnection.prepareStatement("INSERT INTO frame_eeg(record_id, recorded_at, frameCountPerSecond, chk_sum, checksum_valid, raw) VALUES(?, ?, ?, ?, ?, ?)");
            currentAssessmentStatement = dbConnection.prepareStatement("INSERT INTO frame_current_assessment(record_id, recorded_at, eeg_index, emg_index, delta_rel_1, delta_rel_2, theta_rel_1, theta_rel_2, alpha_rel_1, alpha_rel_2, beta_rel_1, beta_rel_2, power_1, power_2, median_1, median_2, edge_freq_1, edge_freq_2, artifacts_1, artifacts_2, alerts , info, bsr_short_1, bsr_medium_1, reserved_1, sti_ch_1, sti_ch_2, bsr_short_2, bsr_medium_2, ibi_ch_1, ibi_ch_2, a_eeg_min_1, a_eeg_max_1, a_eeg_min_2, a_eeg_max_2, reserved_2, chk_sum, checksum_valid, raw) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            powerSpectrumStatement = dbConnection.prepareStatement("INSERT INTO frame_power_spectrum(record_id, recorded_at, info, chk_sum, checksum_valid, raw) VALUES(?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            spectrumStatement = dbConnection.prepareStatement("INSERT INTO power_spectrum(frame_id, channel, 0_0, 0_5, 1_0, 1_5, 2_0, 2_5, 3_0, 3_5, 4_0, 4_5, 5_0, 5_5, 6_0, 6_5, 7_0, 7_5, 8_0, 8_5, 9_0, 9_5, 10_0, 10_5, 11_0, 11_5, 12_0, 12_5, 13_0, 13_5, 14_0, 14_5, 15_0, 15_5, 16_0, 16_5, 17_0, 17_5, 18_0, 18_5, 19_0, 19_5, 20_0, 20_5, 21_0, 21_5, 22_0, 22_5, 23_0, 23_5, 24_0, 24_5, 25_0, 25_5, 26_0, 26_5, 27_0, 27_5, 28_0, 28_5, 29_0, 29_5, 30_0, 30_5, 31_0, 31_5, 32_0, 32_5, 33_0, 33_5, 34_0, 34_5, 35_0, 35_5, 36_0, 36_5, 37_0, 37_5, 38_0, 38_5, 39_0, 39_5, 40_0, 40_5, 41_0, 41_5, 42_0, 42_5, 43_0, 43_5, 44_0, 44_5, 45_0, 45_5, 46_0, 46_5, 47_0, 47_5, 48_0, 48_5, 49_0, 49_5, 50_0, 50_5, 51_0, 51_5, 52_0, 52_5, 53_0, 53_5, 54_0, 54_5, 55_0, 55_5, 56_0, 56_5, 57_0, 57_5, 58_0, 58_5, 59_0, 59_5, 60_0, 60_5, 61_0, 61_5, 62_0, 62_5, 63_0, 63_5) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            electrodeCheckStatement = dbConnection.prepareStatement("INSERT INTO frame_electrode_check(record_id, recorded_at, imp_1_a, imp_1_b, imp_ref, imp_2_a, imp_2_b, info, chk_sum, checksum_valid, raw) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            remainsStatement = dbConnection.prepareStatement("INSERT INTO remains(record_id, recorded_at, raw) VALUES(?, ?, ?)");
        } catch (SQLException e) {
            LOGGER.error("Could not connect to database", e);
            eventManager.dispatchOnRecoverableError();
            application.scheduleRestart();
            System.exit(1);
        }
        Runtime.getRuntime().addShutdownHook(new DatabaseHandlerShutdownHook());
    }

    class DatabaseHandlerShutdownHook extends Thread {
        public void run() {
            LOGGER.warn("DatabaseHandlerShutdownHook triggered");
            try {
                if (dbConnection != null && !dbConnection.isClosed()) {
                    LOGGER.info("Attempting to close DatabaseConnection");
                    dbConnection.close();
                    LOGGER.info("Closed DB connection");
                }
            } catch (Exception e) {
                LOGGER.error("Could not close DatabaseConnection", e);
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
            LOGGER.info("Created recording entry in database, id is {}", recordId);
            isRecording = true;
            eegCounter = 0;
            hasEEGs = false;
            hasCurrentAssessments = false;
            intervalCounter = 0;
        } catch (Exception e) {
            LOGGER.error("Could not create entry for recording", e);
            eventManager.dispatchOnRecoverableError();
            application.scheduleRestart();
            System.exit(1);
        }
    }

    @Override
    public void onRecordingStop() {
        recordId = -1;
        isRecording = false;
        handleBatches();
        intervalCounter = 0;
    }

    @Override
    public void onReceivedEEG(ReceivedEEGEvent event) {
        if (!isRecording) {
            LOGGER.error("Could not process EEGEvent, there is no recording");
            eventManager.dispatchOnRecoverableError();
            application.scheduleRestart();
            System.exit(1);
        }
        try {
            eegStatement.setShort(1, recordId);
            eegStatement.setLong(2, event.getTime());
            eegStatement.setShort(3, eegCounter);
            eegStatement.setBytes(4, event.getCheckSum());
            eegStatement.setBoolean(5, event.isChecksumValid());
            eegStatement.setBytes(6, event.getRaw());
            eegStatement.addBatch();
            if (!hasEEGs) hasEEGs = true;
            eegCounter++;
        } catch (SQLException e) {
            LOGGER.error("Error processing EEG data", e);
            try {
                eegStatement.clearBatch();
                LOGGER.info("Cleared eegBatch");
            } catch (SQLException ex) {
                LOGGER.error("Could not clear EEG Batch", ex);
            } finally {
                eegCounter = 0;
                hasEEGs = false;
            }
        }
    }

    @Override
    public void onReceivedCurrentAssessment(ReceivedCurrentAssessmentEvent event) {
        if (!isRecording) {
            LOGGER.error("Could not process ReceivedCurrentAssessmentEvent, there is no recording");
            eventManager.dispatchOnRecoverableError();
            application.scheduleRestart();
            System.exit(1);
        }
        try {
            currentAssessmentStatement.setShort(1, recordId); // record_id
            currentAssessmentStatement.setLong(2, event.getTime()); // recorded_at
            currentAssessmentStatement.setShort(3, event.getEegIndex()); // eeg_index
            currentAssessmentStatement.setShort(4, event.getEmgIndex()); // emg_index
            currentAssessmentStatement.setFloat(5, event.getDeltaRel1()); // delta_rel_1
            currentAssessmentStatement.setFloat(6, event.getDeltaRel2()); // delta_rel_2
            currentAssessmentStatement.setFloat(7, event.getThetaRel1()); // theta_rel_1
            currentAssessmentStatement.setFloat(8, event.getThetaRel2()); // theta_rel_2
            currentAssessmentStatement.setFloat(9, event.getAlphaRel1()); // alpha_rel_1
            currentAssessmentStatement.setFloat(10, event.getAlphaRel2()); // alpha_rel_2
            currentAssessmentStatement.setFloat(11, event.getBetaRel1()); // beta_rel_1
            currentAssessmentStatement.setFloat(12, event.getBetaRel2()); // beta_rel_2
            currentAssessmentStatement.setFloat(13, event.getPower1()); // power_1
            currentAssessmentStatement.setFloat(14, event.getPower2()); // power_2
            currentAssessmentStatement.setFloat(15, event.getMedian1()); // median_1
            currentAssessmentStatement.setFloat(16, event.getMedian2()); // median_2
            currentAssessmentStatement.setFloat(17, event.getEdgeFreq1()); // edge_freq_1
            currentAssessmentStatement.setFloat(18, event.getEdgeFreq2()); // edge_freq_2
            currentAssessmentStatement.setBytes(19, event.getArtifacts1()); // artifacts_1
            currentAssessmentStatement.setBytes(20, event.getArtifacts2()); // artifacts_2
            currentAssessmentStatement.setByte(21, event.getAlerts()); // alerts
            currentAssessmentStatement.setBytes(22, event.getInfo()); // info
            currentAssessmentStatement.setFloat(23, event.getBsrShort1()); // bsr_short_1
            currentAssessmentStatement.setFloat(24, event.getBsrMedium1()); // bsr_medium_1
            currentAssessmentStatement.setBytes(25, event.getReserved1()); // reserved_1
            currentAssessmentStatement.setByte(26, event.getStiCh1()); // sti_ch_1
            currentAssessmentStatement.setByte(27, event.getStiCh2()); // sti_ch_2
            currentAssessmentStatement.setFloat(28, event.getBsrShort2()); // bsr_short_2
            currentAssessmentStatement.setFloat(29, event.getBsrMedium2()); // bsr_medium_2
            currentAssessmentStatement.setShort(30, event.getIbiCh1()); // ibi_ch_1
            currentAssessmentStatement.setShort(31, event.getIbiCh2()); // ibi_ch_2
            currentAssessmentStatement.setFloat(32, event.getaEEGMin1()); // a_eeg_min_1
            currentAssessmentStatement.setFloat(33, event.getaEEGMax1()); // a_eeg_max_1
            currentAssessmentStatement.setFloat(34, event.getaEEGMin2()); // a_eeg_min_2
            currentAssessmentStatement.setFloat(35, event.getaEEGMax2()); // a_eeg_max_2
            currentAssessmentStatement.setBytes(36, event.getReserved2()); // reserved_2
            currentAssessmentStatement.setBytes(37, event.getCheckSum()); // chk_sum
            currentAssessmentStatement.setBoolean(38, event.isChecksumValid());
            currentAssessmentStatement.setBytes(39, event.getRaw()); // raw
            currentAssessmentStatement.addBatch();
            if (!hasCurrentAssessments) hasCurrentAssessments = true;
        } catch (SQLException e) {
            LOGGER.error("Error processing Current Assessment data", e);
            try {
                currentAssessmentStatement.clearBatch();
            } catch (SQLException ex) {
                LOGGER.error("Could not clear currentAssessment Batch", ex);
            } finally {
                hasCurrentAssessments = false;
            }
        }
    }

    @Override
    public void onReceivedPowerSpectrum(ReceivedPowerSpectrumEvent event) {
        if (!isRecording) {
            LOGGER.error("Could not process ReceivedPowerSpectrumEvent, there is no recording");
            eventManager.dispatchOnRecoverableError();
            application.scheduleRestart();
            System.exit(1);
        }
        try {
            powerSpectrumStatement.setShort(1, recordId);
            powerSpectrumStatement.setLong(2, event.getTime());
            powerSpectrumStatement.setByte(3, event.getInfo());
            powerSpectrumStatement.setBytes(4, event.getCheckSum());
            powerSpectrumStatement.setBoolean(5, event.isChecksumValid());
            powerSpectrumStatement.setBytes(6, event.getRaw());
            if (powerSpectrumStatement.executeUpdate() < 1) {
                LOGGER.error("Error sending block for power spectrum. Did not receive keys as result");
                return;
            }
            ResultSet rs = powerSpectrumStatement.getGeneratedKeys();
            if(rs.next()) {
                int frameId = rs.getInt(1);
                insertSpectrum(frameId, 1, event.getSpectrum1());
                insertSpectrum(frameId, 2, event.getSpectrum2());
            } else {
                LOGGER.error("Error processing power spectrum block. Result has keys, but ResultSet is empty");
            }
            rs.close();
        } catch (SQLException e) {
            LOGGER.error("Error processing Power Spectrum data", e);
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
    public void onReceivedElectrodeCheck(ReceivedElectrodeCheckEvent event) {
        if (!isRecording) {
            LOGGER.error("Could not process ReceivedElectrodeCheckEvent, there is no recording");
            eventManager.dispatchOnRecoverableError();
            application.scheduleRestart();
            System.exit(1);
        }
        try {
            electrodeCheckStatement.setShort(1, recordId);
            electrodeCheckStatement.setLong(2, event.getTime());
            electrodeCheckStatement.setFloat(3, adjustImpedance(event.getImp1a()));
            electrodeCheckStatement.setFloat(4, adjustImpedance(event.getImp1b()));
            electrodeCheckStatement.setFloat(5, adjustImpedance(event.getImpRef()));
            electrodeCheckStatement.setFloat(6, adjustImpedance(event.getImp2a()));
            electrodeCheckStatement.setFloat(7, adjustImpedance(event.getImp2b()));
            electrodeCheckStatement.setByte(8, event.getInfo());
            electrodeCheckStatement.setBytes(9, event.getCheckSum());
            electrodeCheckStatement.setBoolean(10, event.isChecksumValid());
            electrodeCheckStatement.setBytes(11, event.getRaw());
            electrodeCheckStatement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Error processing Electrode Check data", e);
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
    public void onHandleRemains(HandleRemainsEvent event) {
        if (!isRecording) {
            LOGGER.error("Could not process HandleRemainsEvent, there is no recording");
            eventManager.dispatchOnRecoverableError();
            application.scheduleRestart();
            System.exit(1);
        }
        try {
            remainsStatement.setShort(1, recordId);
            remainsStatement.setLong(2, event.getTime());
            for (byte[] chunk: event.getChunks()) {
                remainsStatement.setBytes(3, chunk);
                remainsStatement.addBatch();
            }
            remainsStatement.executeBatch();
            LOGGER.warn("Sent Remains batch");
        } catch (SQLException e) {
            LOGGER.error("Error processing Remains data", e);
        }
    }

    @Override
    public void onIntervalStart() {
        if (!isRecording) return;
        eegCounter = 0;
    }

    @Override
    public void onIntervalEnd() {
        if (!isRecording) return;
        intervalCounter++;
        if (intervalCounter < 10) return;
        intervalCounter = 0;
        handleBatches();
    }

    private void handleBatches() {
        LOGGER.debug("Sending batches to Database");
        if (hasEEGs) {
            try {
                eegStatement.executeBatch();
                LOGGER.debug("Sent EEG batch");
            } catch (SQLException e) {
                LOGGER.error("Could not send EEG batch to database", e);
                try {
                    eegStatement.clearBatch();
                } catch (SQLException ex) {
                    LOGGER.error("Could not clear EEG batch", e);
                    eventManager.dispatchOnRecoverableError();
                    application.scheduleRestart();
                    System.exit(1);
                }
            }
            hasEEGs = false;
        }
        if (hasCurrentAssessments) {
            try {
                currentAssessmentStatement.executeBatch();
                LOGGER.debug("Sent CurrentAssessment batch");
            } catch (SQLException e) {
                LOGGER.error("Could not send CurrentAssessment batch to database", e);
                try {
                    currentAssessmentStatement.clearBatch();
                } catch (SQLException ex) {
                    LOGGER.error("Could not clear CurrentAssessment batch", e);
                    eventManager.dispatchOnRecoverableError();
                    application.scheduleRestart();
                    System.exit(1);
                }
            }
            hasCurrentAssessments = false;
        }
    }
}
