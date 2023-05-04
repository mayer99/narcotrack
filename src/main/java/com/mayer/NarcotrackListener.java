package com.mayer;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListener;
import com.mayer.factory.*;
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
    private final byte  endByte = (byte)0xFE;
    private final ByteBuffer buffer;
    private final ArrayList<NarcotrackFrameListener> listeners;
    private PreparedStatement remainsStatement;
    private final long startTimeLocal;
    private final long startTimeNTP;



    public NarcotrackListener() {
        logger.info("starting...");

        startTimeLocal = System.currentTimeMillis();
        startTimeNTP = getNTPTime();
        if (startTimeNTP > 0) {
            logger.debug("Got both times, difference is {}", startTimeLocal - startTimeNTP);
            if (Math.abs(startTimeLocal - startTimeNTP) > 10000) {
                logger.error("Times differ more than 10s!");
            }
        }

        buffer = ByteBuffer.allocate(50000).order(ByteOrder.LITTLE_ENDIAN);

        listeners = new ArrayList<>();
        try {
            listeners.add(new AddingToDatabaseListener(this));
        } catch (SQLException e) {
            logger.error("Error adding frameListeners to NarcotrackListener", e);
            System.exit(1);
        }
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
        logger.debug("SerialPortEvent (length: {}, buffer position: {})", serialPortEvent.getReceivedData().length, buffer.position());

        if(serialPortEvent.getReceivedData().length + buffer.position() > buffer.capacity()) {
            if (serialPortEvent.getReceivedData().length > buffer.capacity()) {
                logger.error("Received more data than the buffer can hold (length: {}, buffer size: {}). Moving data and buffer to remains", serialPortEvent.getReceivedData().length, buffer.capacity());
                final Remains bufferRemains = new Remains(getTimeDifference(), buffer);
                final Remains eventRemains = new Remains(getTimeDifference(), serialPortEvent.getReceivedData());
                listeners.forEach(listener -> listener.onRemains(bufferRemains));
                listeners.forEach(listener -> listener.onRemains(eventRemains));
                return;
            }
            logger.error("Buffer cannot hold received data (length of received data: {}, buffer position: {}, buffer capacity: {}). Clearing buffer and processing newly received data", serialPortEvent.getReceivedData().length, buffer.position(), buffer.capacity());
            final Remains bufferRemains = new Remains(getTimeDifference(), buffer);
            listeners.forEach(listener -> listener.onRemains(bufferRemains));
        }
        buffer.put(serialPortEvent.getReceivedData());

        // Matching Package Types
        if (buffer.position() < ElectrodeCheck.getLength()) {
            logger.debug("Received fragment of {} bytes (warning: fragments longer than 28 bytes are still possible)", buffer.position());
            return;
        }

        if (EEG.detect(buffer)) {
            final EEG eeg = new EEG(getTimeDifference(), buffer);
            listeners.forEach(listener -> listener.onEEG(eeg));
            if (buffer.position() > 0) {
                final Remains bufferRemains = new Remains(getTimeDifference(), buffer);
                listeners.forEach(listener -> listener.onRemains(bufferRemains));
            }
            return;
        }

        if (CurrentAssessment.detect(buffer)) {
            final CurrentAssessment currentAssessment = new CurrentAssessment(getTimeDifference(), buffer);
            listeners.forEach(listener -> listener.onCurrentAssessment(currentAssessment));
            if (buffer.position() > 0) {
                final Remains bufferRemains = new Remains(getTimeDifference(), buffer);
                listeners.forEach(listener -> listener.onRemains(bufferRemains));
            }
            return;
        }

        if (PowerSpectrum.detect(buffer)) {
            final PowerSpectrum powerSpectrum = new PowerSpectrum(getTimeDifference(), buffer);
            listeners.forEach(listener -> listener.onPowerSpectrum(powerSpectrum));
            if (buffer.position() > 0) {
                final Remains bufferRemains = new Remains(getTimeDifference(), buffer);
                listeners.forEach(listener -> listener.onRemains(bufferRemains));
            }
            return;
        }

        if (ElectrodeCheck.detect(buffer)) {
            final ElectrodeCheck electrodeCheck = new ElectrodeCheck(getTimeDifference(), buffer);
            listeners.forEach(listener -> listener.onElectrodeCheck(electrodeCheck));
            if (buffer.position() > 0) {
                final Remains bufferRemains = new Remains(getTimeDifference(), buffer);
                listeners.forEach(listener -> listener.onRemains(bufferRemains));
            }
            return;
        }

        if(buffer.position() > 0) {
            logger.warn("Received data than could not be matched to a handler. Buffer-Length is {}", buffer.position());
        }
    }

    private long getNTPTime() {
        final NTPUDPClient client = new NTPUDPClient();
        client.setDefaultTimeout(1000);
        try {
            client.open();
            final String[] ntpHosts = new String[]{"0.de.pool.ntp.org", "1.de.pool.ntp.org", "2.de.pool.ntp.org"};
            //final String[] ntpHosts = new String[]{};
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
            return 0;
        } catch (IOException e) {
            logger.warn("Error creating NTPUDPClient", e);
            return 0;
        } finally {
            if (client != null && client.isOpen()) {
                client.close();
            }
        }
    }

    public long getStartTimeNTP() {
        return startTimeNTP;
    }
    public long getStartTimeLocal() {
        return startTimeLocal;
    }

    private int getTimeDifference() {
        return Math.toIntExact(System.currentTimeMillis() - startTimeLocal);
    }
}
