package com.mayer99.narcotrack.handlers;

import com.mayer99.Narcotrack;
import com.mayer99.narcotrack.base.events.CurrentAssessmentEvent;
import com.mayer99.narcotrack.base.events.EEGEvent;
import com.mayer99.narcotrack.base.events.ElectrodeCheckEvent;
import com.mayer99.narcotrack.base.events.PowerSpectrumEvent;
import com.mayer99.narcotrack.base.events.RemainsEvent;
import com.mayer99.narcotrack.base.frames.CurrentAssessment;
import com.mayer99.narcotrack.base.frames.ElectrodeCheck;
import com.mayer99.narcotrack.base.frames.PowerSpectrum;
import com.mayer99.narcotrack.base.frames.Remains;
import com.mayer99.narcotrack.base.models.NarcotrackEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;
import java.util.Arrays;

public class MariaDatabaseHandler implements NarcotrackEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MariaDatabaseHandler.class);

    private final Instant startTime;
    private Connection databaseConnection;
    private int recordId;
    private PreparedStatement eegStatement, currentAssessmentStatement, powerSpectrumStatement, spectrumStatement, electrodeCheckStatement, remainsStatement;
    private int eegCounter = 0;
    private int intervalCounter = 0;
    private boolean hasEEGs = false;
    private boolean hasCurrentAssessments = false;


    public MariaDatabaseHandler(Narcotrack narcotrack) {
        startTime = narcotrack.getStartTime();

        String DB_URL = System.getenv("DB_URL");
        String DB_USERNAME = System.getenv("DB_USERNAME");
        String DB_PASSWORD = System.getenv("DB_PASSWORD");

        if (DB_URL == null || DB_URL.trim().isEmpty()) {
            LOGGER.error("DB_URL is null, empty or whitespace");
            Narcotrack.rebootPlatform();
            return;
        }

        if (DB_USERNAME == null || DB_USERNAME.trim().isEmpty()) {
            LOGGER.error("DB_USERNAME is null, empty or whitespace");
            Narcotrack.rebootPlatform();
            return;
        }

        if (DB_PASSWORD == null || DB_PASSWORD.trim().isEmpty()) {
            LOGGER.error("DB_PASSWORD is null, empty or whitespace");
            Narcotrack.rebootPlatform();
            return;
        }

        try {
            LOGGER.debug("Connecting to database {}", DB_URL);
            // "jdbc:mariadb://127.0.0.1:3306/narcotrack"
            databaseConnection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
            Runtime.getRuntime().addShutdownHook(new DatabaseShutdownHook());

            eegStatement = databaseConnection.prepareStatement("INSERT INTO frame_eeg(record_id, recorded_at, frameCountPerSecond, raw) VALUES(?, ?, ?, ?)");
            currentAssessmentStatement = databaseConnection.prepareStatement("INSERT INTO frame_current_assessment(record_id, recorded_at, eeg_index, emg_index, delta_rel_1, delta_rel_2, theta_rel_1, theta_rel_2, alpha_rel_1, alpha_rel_2, beta_rel_1, beta_rel_2, power_1, power_2, median_1, median_2, edge_freq_1, edge_freq_2, artifacts_1, artifacts_2, alerts , info, bsr_short_1, bsr_medium_1, reserved_1, sti_ch_1, sti_ch_2, bsr_short_2, bsr_medium_2, ibi_ch_1, ibi_ch_2, a_eeg_min_1, a_eeg_max_1, a_eeg_min_2, a_eeg_max_2, reserved_2, chk_sum, raw) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            powerSpectrumStatement = databaseConnection.prepareStatement("INSERT INTO frame_power_spectrum(record_id, recorded_at, info, chk_sum, raw) VALUES(?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            spectrumStatement = databaseConnection.prepareStatement("INSERT INTO power_spectrum(frame_id, channel, 0_0, 0_5, 1_0, 1_5, 2_0, 2_5, 3_0, 3_5, 4_0, 4_5, 5_0, 5_5, 6_0, 6_5, 7_0, 7_5, 8_0, 8_5, 9_0, 9_5, 10_0, 10_5, 11_0, 11_5, 12_0, 12_5, 13_0, 13_5, 14_0, 14_5, 15_0, 15_5, 16_0, 16_5, 17_0, 17_5, 18_0, 18_5, 19_0, 19_5, 20_0, 20_5, 21_0, 21_5, 22_0, 22_5, 23_0, 23_5, 24_0, 24_5, 25_0, 25_5, 26_0, 26_5, 27_0, 27_5, 28_0, 28_5, 29_0, 29_5, 30_0, 30_5, 31_0, 31_5, 32_0, 32_5, 33_0, 33_5, 34_0, 34_5, 35_0, 35_5, 36_0, 36_5, 37_0, 37_5, 38_0, 38_5, 39_0, 39_5, 40_0, 40_5, 41_0, 41_5, 42_0, 42_5, 43_0, 43_5, 44_0, 44_5, 45_0, 45_5, 46_0, 46_5, 47_0, 47_5, 48_0, 48_5, 49_0, 49_5, 50_0, 50_5, 51_0, 51_5, 52_0, 52_5, 53_0, 53_5, 54_0, 54_5, 55_0, 55_5, 56_0, 56_5, 57_0, 57_5, 58_0, 58_5, 59_0, 59_5, 60_0, 60_5, 61_0, 61_5, 62_0, 62_5, 63_0, 63_5) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            electrodeCheckStatement = databaseConnection.prepareStatement("INSERT INTO frame_electrode_check(record_id, recorded_at, imp_1_a, imp_1_b, imp_ref, imp_2_a, imp_2_b, info, chk_sum, raw) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            remainsStatement = databaseConnection.prepareStatement("INSERT INTO remains(record_id, recorded_at, raw) VALUES(?, ?, ?)");
            createRecording();
        } catch (Exception e) {
            LOGGER.error("Could not start MariaDatabaseHandler", e);
            Narcotrack.rebootPlatform();
        }
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
                LOGGER.error("Error closing database connection", e);
            }
        }
    }

    public void createRecording() throws Exception {
        PreparedStatement insertRecording = databaseConnection.prepareStatement("INSERT INTO recordings(start_time) VALUES(?)", Statement.RETURN_GENERATED_KEYS);
        insertRecording.setTimestamp(1, Timestamp.from(startTime));
        if(insertRecording.executeUpdate() < 1) {
            throw new Exception("Could not insert recording, got no rows back");
        }
        ResultSet rs = insertRecording.getGeneratedKeys();
        if(!rs.next()) {
            throw new Exception("Insert of recording did not return an id key");
        }
        recordId = rs.getInt(1);
        rs.close();
        insertRecording.close();
        LOGGER.info("Created recording, id is {}", recordId);
    }


    @Override
    public void onEEGEvent(EEGEvent event) {

        try {
            eegStatement.setInt(1, recordId);
            eegStatement.setLong(2, event.getTime());
            eegStatement.setInt(3, eegCounter);
            eegStatement.setBytes(4, event.getData().getRaw());
            eegStatement.addBatch();
            if (!hasEEGs) hasEEGs = true;
            eegCounter++;
            LOGGER.debug("Created EEG batch");
        } catch (SQLException e) {
            LOGGER.error("Error processing EEG data", e);
            try {
                eegStatement.clearBatch();
                eegCounter = 0;
            } catch (SQLException ex) {
                LOGGER.error("Could not clear EEG Batch", ex);
            }
        }
    }

    @Override
    public void onCurrentAssessmentEvent(CurrentAssessmentEvent event) {

        try {
            CurrentAssessment data = event.getData();
            currentAssessmentStatement.setInt(1, recordId); // record_id
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
            currentAssessmentStatement.setShort(26, data.getStiCh1()); // sti_ch_1
            currentAssessmentStatement.setShort(27, data.getStiCh2()); // sti_ch_2
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
            currentAssessmentStatement.setBytes(38, data.getRaw()); // raw
            currentAssessmentStatement.addBatch();
            if (!hasCurrentAssessments) hasCurrentAssessments = true;
            LOGGER.debug("Created Current Assessment batch");
        } catch (SQLException e) {
            LOGGER.error("Error processing Current Assessment data", e);
            try {
                currentAssessmentStatement.clearBatch();
            } catch (SQLException ex) {
                LOGGER.error("Could not clear currentAssessment Batch", ex);
            }
        }
    }

    @Override
    public void onPowerSpectrumEvent(PowerSpectrumEvent event) {

        try {
            PowerSpectrum data = event.getData();
            powerSpectrumStatement.setInt(1, recordId);
            powerSpectrumStatement.setLong(2, event.getTime());
            powerSpectrumStatement.setByte(3, data.getInfo());
            powerSpectrumStatement.setBytes(4, data.getChkSum());
            powerSpectrumStatement.setBytes(5, data.getRaw());
            if (powerSpectrumStatement.executeUpdate() < 1) {
                LOGGER.error("Error sending block for power spectrum. Did not receive keys as result");
                return;
            }
            ResultSet rs = powerSpectrumStatement.getGeneratedKeys();
            if(rs.next()) {
                int frameId = rs.getInt(1);
                insertSpectrum(frameId, 1, data.getSpectrum1());
                insertSpectrum(frameId, 2, data.getSpectrum2());
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
    public void onElectrodeCheckEvent(ElectrodeCheckEvent event) {
        try {
            ElectrodeCheck data = event.getData();
            electrodeCheckStatement.setInt(1, recordId);
            electrodeCheckStatement.setLong(2, event.getTime());
            electrodeCheckStatement.setFloat(3, adjustImpedance(data.getImp1a()));
            electrodeCheckStatement.setFloat(4, adjustImpedance(data.getImp1b()));
            electrodeCheckStatement.setFloat(5, adjustImpedance(data.getImpRef()));
            electrodeCheckStatement.setFloat(6, adjustImpedance(data.getImp2a()));
            electrodeCheckStatement.setFloat(7, adjustImpedance(data.getImp2b()));
            electrodeCheckStatement.setByte(8, data.getInfo());
            electrodeCheckStatement.setBytes(9, data.getChkSum());
            electrodeCheckStatement.setBytes(10, data.getRaw());
            electrodeCheckStatement.executeUpdate();
            LOGGER.debug("Sent Electrode Check");
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
    public void onRemainsEvent(RemainsEvent event) {

        try {
            Remains data = event.getData();
            remainsStatement.setInt(1, recordId);
            remainsStatement.setLong(2, event.getTime());
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

    @Override
    public void onEndOfInterval() {
        eegCounter = 0;
        intervalCounter++;
        int MAX_INTERVALS = 10;
        if (intervalCounter >= MAX_INTERVALS) {
            intervalCounter = 0;
            try {
                if (hasEEGs) {
                    eegStatement.executeBatch();
                    LOGGER.debug("Sent EEG batch");
                }
                if (hasCurrentAssessments) {
                    currentAssessmentStatement.executeBatch();
                    LOGGER.debug("Sent CurrentAssessment batch");
                }
            } catch (SQLException e) {
                LOGGER.error("Error processing EEG or CurrentAssessment Frame", e);
                try {
                    eegStatement.clearBatch();
                    currentAssessmentStatement.clearBatch();
                } catch (SQLException ex) {
                    LOGGER.error("Could not clear batches", e);
                }
            } finally {
                hasEEGs = false;
                hasCurrentAssessments = false;
            }
        }
    }
}
