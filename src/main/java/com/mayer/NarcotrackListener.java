package com.mayer;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListener;
import com.mayer.handler.*;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;

public class NarcotrackListener implements SerialPortMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(NarcotrackListener.class);
    private final Connection databaseConnection;
    private final byte  endByte = (byte)0xFE;
    private final ByteBuffer buffer;
    private final ArrayList<NarcotrackFrameHandler> handlerList;
    private PreparedStatement remainsStatement;
    private final long startTimeLocal;
    private final long startTimeNTP;
    private int recordId;
    private final NarcotrackStatistics statistics;



    public NarcotrackListener(Connection databaseConnection) {
        logger.info("starting...");
        this.databaseConnection = databaseConnection;
        startTimeLocal = System.currentTimeMillis();
        startTimeNTP = getNTPTime();

        if (startTimeNTP > 0) {
            logger.debug("Got both times, difference is {}", startTimeLocal - startTimeNTP);
            if (Math.abs(startTimeLocal - startTimeNTP) > 10000) {
                logger.error("Times differ more than 10s!");
            }
        }

        try {
            PreparedStatement statement = databaseConnection.prepareStatement("INSERT INTO recordings(start_time_local, start_time_ntp) VALUES(?, ?)", Statement.RETURN_GENERATED_KEYS);
            statement.setTimestamp(1, new Timestamp(startTimeLocal));
            if (startTimeNTP > 0) {
                statement.setTimestamp(2, new Timestamp(startTimeNTP));
            } else {
                statement.setNull(2, Types.NULL);
            }
            if(statement.executeUpdate() < 1) {
                logger.error("Could not insert recording, got no rows back");
                System.exit(1);
            }
            ResultSet rs = statement.getGeneratedKeys();
            if(!rs.next()) {
                logger.error("Insert of recording did not return an id key");
                System.exit(1);
            }
            recordId = rs.getInt(1);
            // recordId = rs.getInt("insert_id");
            rs.close();
        } catch (SQLException e) {
            logger.error("Error creating recording", e);
            System.exit(1);
        }
        logger.info("Created recording, id is {}", recordId);
        buffer = ByteBuffer.allocate(50000).order(ByteOrder.LITTLE_ENDIAN);
        try {
            remainsStatement = databaseConnection.prepareStatement("INSERT INTO remains(record_id, recorded_at, raw) VALUES(?, ?, ?)");
        } catch (SQLException e) {
            logger.error("Could not create remainsStatement", e);
            System.exit(1);
        }
        handlerList = new ArrayList<>();
        try {
            handlerList.add(new NarcotrackEEGHandler(this));
            handlerList.add(new NarcotrackCurrentAssessmentHandler(this));
            handlerList.add(new NarcotrackPowerSpectrumHandler(this));
            handlerList.add(new NarcotrackElectrodeCheckHandler(this));
        } catch (SQLException e) {
            logger.error("SQLEx in one of the handlers", e);
            System.exit(1);
        }
        statistics = new NarcotrackStatistics(recordId, startTimeLocal, startTimeNTP);
    }

    @Override
    public byte[] getMessageDelimiter() {
        return new byte[] {endByte};
    }

    @Override
    public boolean delimiterIndicatesEndOfMessage() {
        return true;
    }

    @Override
    public int getListeningEvents() {
        return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
    }

    @Override
    public void serialEvent(SerialPortEvent serialPortEvent) {
        System.out.println("Check des Listeners");
        logger.debug("SerialPortEvent, length of data is {}. Buffer position: {}", serialPortEvent.getReceivedData().length, buffer.position());
        if(serialPortEvent.getReceivedData().length + buffer.position() > buffer.capacity()) {
            if (serialPortEvent.getReceivedData().length > buffer.capacity()) {
                logger.error("Received more data than can fit in the buffer. Length of getReceivedData() is {}. Moving data and buffer to remains", serialPortEvent.getReceivedData().length);
                moveBufferToRemains();
                moveDataToRemains(serialPortEvent.getReceivedData());
                return;
            }
            logger.error("Data does not fit in buffer. Clearing buffer and processing newly received data");
            moveBufferToRemains();
        }
        buffer.put(serialPortEvent.getReceivedData());

        for(NarcotrackFrameHandler handler: handlerList) {
            if(handler.detected(buffer)) {
                handler.process(buffer);
                return;
            }
        }

        if(buffer.position() > 0) {
            logger.warn("Received data than could not be matched to a handler. Buffer-Length is {}", buffer.position());
        }
    }

    public void moveBufferToRemains() {
        moveBufferToRemains(0);
    }

    public void moveBufferToRemains(int delimiter) {
        int endPosition = buffer.position() - delimiter;
        buffer.position(0);
        try {
            remainsStatement.setInt(1, recordId);
            remainsStatement.setInt(2, Math.toIntExact(System.currentTimeMillis() - startTimeLocal));
            if (endPosition > 1000) {
                byte[] chunk = new byte[1000];
                while (endPosition - buffer.position() > 1000) {
                    buffer.get(chunk);
                    remainsStatement.setBytes(3, chunk);
                    remainsStatement.addBatch();
                }
            }
            byte[] data = new byte[endPosition - buffer.position()];
            buffer.get(data);
            remainsStatement.setBytes(3, data);
            if (endPosition > 1000) {
                remainsStatement.addBatch();
                remainsStatement.executeBatch();
            } else {
                remainsStatement.executeUpdate();
            }
            logger.debug("Sent remains to database");

        } catch (SQLException e) {
            logger.error("Could not insert remains into database", e);
        } finally {
            buffer.clear();
        }
    }

    public void moveDataToRemains(byte[] data) {
        try {
            remainsStatement.setInt(1, recordId);
            remainsStatement.setInt(2, Math.toIntExact(System.currentTimeMillis() - startTimeLocal));
            if (data.length <= 1000) {
                remainsStatement.setBytes(3, data);
                remainsStatement.executeUpdate();
                logger.debug("Sent remains to database");
            }
            byte[] chunk;
            for (int i = 0; i < data.length; i = i + 1000) {
                chunk = Arrays.copyOfRange(data, i, (i + 1000 > data.length ? data.length - i : i + 1000));
                logger.info("Chunk size is {}", chunk.length);
            }
            logger.debug("Sent remains to database");
        } catch (SQLException e) {
            logger.error("Could not send byte[] from getReceivedData() to database", e);
        }
    }

    public Connection getDatabaseConnection() {
        return databaseConnection;
    }

    private long getNTPTime() {
        final NTPUDPClient client = new NTPUDPClient();
        client.setDefaultTimeout(10000);
        try {
            client.open();
            // final String[] ntpHosts = new String[]{"0.de.pool.ntp.org", "1.de.pool.ntp.org", "2.de.pool.ntp.org"};
            final String[] ntpHosts = new String[]{};
            for(String ntpHost: ntpHosts) {
                try {
                    InetAddress hostAddr = InetAddress.getByName(ntpHost);
                    return client
                            .getTime(hostAddr)
                            .getMessage()
                            .getTransmitTimeStamp()
                            .getTime();
                } catch (IOException e) {
                    logger.warn("Could not connect to ntp server {}", ntpHost);
                }
            }
        } catch (IOException e) {
            logger.warn("Could not connect to NTP servers");
        } finally {
            if (client != null && client.isOpen()) {
                client.close();
            }
        }
        return -1;
    }

    public long getStartTime() {
        return startTimeLocal;
    }

    public int getRecordId() {
        return recordId;
    }

    public NarcotrackStatistics getStatistics() {
        return statistics;
    }
}
