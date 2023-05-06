package com.mayer.listeners;

import com.mayer.NarcotrackListener;
import com.mayer.factory.NarcotrackFrameListener;
import com.mayer.Remains;
import com.mayer.frames.CurrentAssessment;
import com.mayer.frames.EEG;
import com.mayer.frames.ElectrodeCheck;
import com.mayer.frames.PowerSpectrum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Arrays;

public class MariaDatabaseListener implements NarcotrackFrameListener {

    private static final Logger logger = LoggerFactory.getLogger(MariaDatabaseListener.class);
    private final String NARCOTRACK_DB_URL = System.getenv("NARCOTRACK_DB_URL");
    private final String NARCOTRACK_DB_TABLE = System.getenv("NARCOTRACK_DB_TABLE");
    private final String NARCOTRACK_DB_USERNAME = System.getenv("NARCOTRACK_DB_USERNAME");
    private final String NARCOTRACK_DB_PASSWORD = System.getenv("NARCOTRACK_DB_PASSWORD");
    private final NarcotrackListener narcotrackListener;
    private Connection databaseConnection;
    private int recordId;
    private final PreparedStatement recordingsStatement;
    private final PreparedStatement eegStatement;
    private final PreparedStatement currentAssessmentStatement;
    private final PreparedStatement powerSpectrumStatement;
    private final PreparedStatement spectrumStatement;
    private final PreparedStatement electrodeCheckStatement;
    private final PreparedStatement remainsStatement;
    private int eegBatchCounter = 0;
    private int currentAssessmentBatchCounter = 0;

    public MariaDatabaseListener(NarcotrackListener narcotrackListener) throws SQLException {
        this.narcotrackListener = narcotrackListener;

        logger.debug("Connecting to database {}", NARCOTRACK_DB_URL);
        databaseConnection = DriverManager.getConnection("jdbc:mariadb://" + NARCOTRACK_DB_URL + ":3306/" + NARCOTRACK_DB_TABLE, NARCOTRACK_DB_USERNAME, NARCOTRACK_DB_PASSWORD);
        Runtime.getRuntime().addShutdownHook(new DatabaseShutdownHook(databaseConnection));

        recordingsStatement = databaseConnection.prepareStatement("INSERT INTO recordings(start_time_local, start_time_ntp) VALUES(?, ?)", Statement.RETURN_GENERATED_KEYS);
        eegStatement = databaseConnection.prepareStatement("INSERT INTO eeg_frame(record_id, recorded_at, raw) VALUES(?, ?, ?)");
        currentAssessmentStatement = databaseConnection.prepareStatement("INSERT INTO current_assessment_frame(record_id, recorded_at, eeg_index, emg_index, delta_rel_1, delta_rel_2, theta_rel_1, theta_rel_2, alpha_rel_1, alpha_rel_2, beta_rel_1, beta_rel_2, power_1, power_2, median_1, median_2, edge_freq_1, edge_freq_2, artifacts_1, artifacts_2, alerts , info, bsr_short_1, bsr_medium_1, reserved_1, sti_ch_1, sti_ch_2, bsr_short_2, bsr_medium_2, ibi_ch_1, ibi_ch_2, a_eeg_min_1, a_eeg_max_1, a_eeg_min_2, a_eeg_max_2, reserved_2, chk_sum, raw) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        powerSpectrumStatement = databaseConnection.prepareStatement("INSERT INTO power_spectrum_frame(record_id, recorded_at, info, chk_sum, raw) VALUES(?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
        spectrumStatement = databaseConnection.prepareStatement("INSERT INTO power_spectrum(frame_id, channel, 0_0, 0_5, 1_0, 1_5, 2_0, 2_5, 3_0, 3_5, 4_0, 4_5, 5_0, 5_5, 6_0, 6_5, 7_0, 7_5, 8_0, 8_5, 9_0, 9_5, 10_0, 10_5, 11_0, 11_5, 12_0, 12_5, 13_0, 13_5, 14_0, 14_5, 15_0, 15_5, 16_0, 16_5, 17_0, 17_5, 18_0, 18_5, 19_0, 19_5, 20_0, 20_5, 21_0, 21_5, 22_0, 22_5, 23_0, 23_5, 24_0, 24_5, 25_0, 25_5, 26_0, 26_5, 27_0, 27_5, 28_0, 28_5, 29_0, 29_5, 30_0, 30_5, 31_0, 31_5, 32_0, 32_5, 33_0, 33_5, 34_0, 34_5, 35_0, 35_5, 36_0, 36_5, 37_0, 37_5, 38_0, 38_5, 39_0, 39_5, 40_0, 40_5, 41_0, 41_5, 42_0, 42_5, 43_0, 43_5, 44_0, 44_5, 45_0, 45_5, 46_0, 46_5, 47_0, 47_5, 48_0, 48_5, 49_0, 49_5, 50_0, 50_5, 51_0, 51_5, 52_0, 52_5, 53_0, 53_5, 54_0, 54_5, 55_0, 55_5, 56_0, 56_5, 57_0, 57_5, 58_0, 58_5, 59_0, 59_5, 60_0, 60_5, 61_0, 61_5, 62_0, 62_5, 63_0, 63_5) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?");
        electrodeCheckStatement = databaseConnection.prepareStatement("INSERT INTO electrode_check_frame(record_id, recorded_at, imp_1_a, imp_1_b, imp_ref, imp_2_a, imp_2_b, info, chk_sum, raw) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        remainsStatement = databaseConnection.prepareStatement("INSERT INTO remains(record_id, recorded_at, raw) VALUES(?, ?, ?)");

        createRecording();


    }

    class DatabaseShutdownHook extends Thread {

        private final Connection databaseConnection;

        public DatabaseShutdownHook(Connection databaseConnection) {
            this.databaseConnection = databaseConnection;
        }

        public void run() {
            try {
                if (databaseConnection != null && !databaseConnection.isClosed()) {
                    databaseConnection.close();
                    logger.info("Closed DB connection");
                }
            } catch (SQLException e) {
                logger.error("Error closing database connection", e);
            }
        }
    }

    public void createRecording() throws SQLException {
        recordingsStatement.setTimestamp(1, new Timestamp(narcotrackListener.getStartTimeLocal()));
        if (narcotrackListener.getStartTimeNTP() > 0) {
            recordingsStatement.setTimestamp(2, new Timestamp(narcotrackListener.getStartTimeNTP()));
        } else {
            recordingsStatement.setNull(2, Types.NULL);
        }
        if(recordingsStatement.executeUpdate() < 1) {
            logger.error("Could not insert recording, got no rows back");
            System.exit(1);
        }
        ResultSet rs = recordingsStatement.getGeneratedKeys();
        if(!rs.next()) {
            logger.error("Insert of recording did not return an id key");
            System.exit(1);
        }
        recordId = rs.getInt(1);
        // recordId = rs.getInt("insert_id");
        rs.close();
        logger.info("Created recording, id is {}", recordId);
    }


    @Override
    public void onEEG(EEG data) {

        try {
            eegStatement.setInt(1, recordId);
            eegStatement.setInt(2, data.getTime());
            eegStatement.setBytes(3, data.getRaw());
            eegStatement.addBatch();
            logger.debug("Created EEG batch");
            eegBatchCounter++;

            if (eegBatchCounter >= 0) {
                eegStatement.executeBatch();
                eegBatchCounter = 0;
                logger.debug("Sent EEG batch");
            }
        } catch (SQLException e) {
            logger.error("Error processing EEG data", e);
        }

    }

    @Override
    public void onCurrentAssessment(CurrentAssessment data) {

        try {
            currentAssessmentStatement.setInt(1, recordId); // record_id
            currentAssessmentStatement.setInt(2, data.getTime()); // recorded_at
            currentAssessmentStatement.setShort(3, data.getEegIndex()); // eeg_index
            currentAssessmentStatement.setShort(4, data.getEmgIndex()); // emg_index
            currentAssessmentStatement.setFloat(5, data.getDeltaRel1()); // delta_rel_1
            currentAssessmentStatement.setFloat(6, data.getDeltaRel2()); // delta_rel_2
            currentAssessmentStatement.setFloat(7, data.getThetaRel1()); // theta_rel_1
            currentAssessmentStatement.setFloat(8, data.getThetaRel2()); // theta_rel_2
            currentAssessmentStatement.setFloat(9, data.getAlphaRel1()); // alpha_rel_1
            currentAssessmentStatement.setFloat(10, data.getThetaRel2()); // alpha_rel_2
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
            currentAssessmentStatement.setFloat(32, data.getaEEGmin1()); // a_eeg_min_1
            currentAssessmentStatement.setFloat(33, data.getaEEGmax1()); // a_eeg_max_1
            currentAssessmentStatement.setFloat(34, data.getaEEGmin2()); // a_eeg_min_2
            currentAssessmentStatement.setFloat(35, data.getaEEGmax2()); // a_eeg_max_2
            currentAssessmentStatement.setBytes(36, data.getReserved2()); // reserved_2
            currentAssessmentStatement.setBytes(37, data.getChkSum()); // chk_sum
            currentAssessmentStatement.setBytes(38, data.getRaw()); // raw
            currentAssessmentStatement.addBatch();
            logger.debug("Created Current Assessment batch");
            currentAssessmentBatchCounter++;

            if(currentAssessmentBatchCounter >= 0) {
                currentAssessmentStatement.executeBatch();
                currentAssessmentBatchCounter = 0;
                logger.debug("Sent Current Assessment batch");
            }
        } catch (SQLException e) {
            logger.error("Error processing Current Assessment data", e);
        }
    }

    @Override
    public void onPowerSpectrum(PowerSpectrum data) {

        try {
            powerSpectrumStatement.setInt(1, recordId);
            powerSpectrumStatement.setInt(2, data.getTime());
            powerSpectrumStatement.setByte(3, data.getInfo());
            powerSpectrumStatement.setBytes(4, data.getChkSum());
            powerSpectrumStatement.setBytes(5, data.getRaw());
            if (powerSpectrumStatement.executeUpdate() > 0) {
                ResultSet rs = powerSpectrumStatement.getGeneratedKeys();
                if(rs.next()) {
                    final int frameId = rs.getInt(1);
                    insertSpectrum(frameId, 1, data.getSpectrum1());
                    insertSpectrum(frameId, 2, data.getSpectrum2());
                } else {
                    logger.error("Error processing power spectrum block. Result has keys, but resultset cannot be moved to next()");
                }
                rs.close();
            } else {
                logger.error("Error sending block for power spectrum. Did not receive keys as result");
            }
            logger.debug("Sent Power Spectrum");
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
    public void onElectrodeCheck(ElectrodeCheck data) {

        try {
            electrodeCheckStatement.setInt(1, recordId);
            electrodeCheckStatement.setInt(2, data.getTime());
            electrodeCheckStatement.setFloat(3, data.getImp1a());
            electrodeCheckStatement.setFloat(4, data.getImp1b());
            electrodeCheckStatement.setFloat(5, data.getImpRef());
            electrodeCheckStatement.setFloat(6, data.getImp2a());
            electrodeCheckStatement.setFloat(7, data.getImp2b());
            electrodeCheckStatement.setByte(8, data.getInfo());
            electrodeCheckStatement.setBytes(9, data.getChkSum());
            electrodeCheckStatement.setBytes(10, data.getRaw());
            electrodeCheckStatement.executeUpdate();
            logger.debug("Sent Electrode Check");
        } catch (SQLException e) {
            logger.error("Error processing Electrode Check data", e);
        }

    }

    @Override
    public void onRemains(Remains data) {

        try {
            remainsStatement.setInt(1, recordId);
            remainsStatement.setInt(2, data.getTime());
            if (data.getData().size() > 1) {
                for (byte[] chunk: data.getData()) {
                    remainsStatement.setBytes(3, chunk);
                    remainsStatement.addBatch();
                }
                remainsStatement.executeBatch();
                logger.debug("Sent Remains batch");
            } else {
                remainsStatement.setBytes(3, data.getData().get(0));
                remainsStatement.executeUpdate();
                logger.debug("Sent Remains");
            }
        } catch (SQLException e) {
            logger.error("Error processing Remains data", e);
        }
    }
}
