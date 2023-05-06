package com.mayer;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListener;
import com.mayer.factory.*;
import com.mayer.frames.*;
import com.mayer.listeners.MariaDatabaseListener;
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
import java.util.Comparator;

public class NarcotrackListener implements SerialPortMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(NarcotrackListener.class);
    private static final byte  startByte = (byte)0xFF;
    private final byte  endByte = (byte)0xFE;
    private final ByteBuffer buffer;
    private final NarcotrackFrames shortestFrame;
    private final ArrayList<NarcotrackFrameListener> listeners;
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
        shortestFrame = Arrays.stream(NarcotrackFrames.values()).min(Comparator.comparing(NarcotrackFrames::getLength)).get();
        if (shortestFrame == null) {
            logger.error("No entrys in enum NarcotrackFrames");
            System.exit(1);
        }

        listeners = new ArrayList<>();
        try {
            listeners.add(new MariaDatabaseListener(this));
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
        if (buffer.position() < shortestFrame.getLength()) {
            logger.debug("Received fragment of {} bytes", buffer.position());
            return;
        }

        for (NarcotrackFrames frame: NarcotrackFrames.values()) {
            if (buffer.position() < frame.getLength()) continue;
            if (buffer.get(buffer.position() - frame.getLength() + 3) != frame.getIdentifier()) continue;
            if (buffer.get(buffer.position() - frame.getLength()) != startByte) continue;

            logger.debug("Found a frame of type {} in the buffer", frame);

            switch (frame) {
                case EEG:
                    final EEG eeg = new EEG(getTimeDifference(), buffer);
                    listeners.forEach(narcotrackFrameListener -> narcotrackFrameListener.onEEG(eeg));
                    break;
                case CURRENT_ASSESSMENT:
                    final CurrentAssessment currentAssessment = new CurrentAssessment(getTimeDifference(), buffer);
                    listeners.forEach(narcotrackFrameListener -> narcotrackFrameListener.onCurrentAssessment(currentAssessment));
                    break;
                case POWER_SPECTRUM:
                    final PowerSpectrum powerSpectrum = new PowerSpectrum(getTimeDifference(), buffer);
                    listeners.forEach(narcotrackFrameListener -> narcotrackFrameListener.onPowerSpectrum(powerSpectrum));
                    break;
                case ELECTRODE_CHECK:
                    final ElectrodeCheck electrodeCheck = new ElectrodeCheck(getTimeDifference(), buffer);
                    listeners.forEach(narcotrackFrameListener -> narcotrackFrameListener.onElectrodeCheck(electrodeCheck));
                    break;
            }

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
        client.setDefaultTimeout(2000);
        try {
            client.open();
            final String[] ntpHosts = new String[]{"0.de.pool.ntp.org", "1.de.pool.ntp.org", "2.de.pool.ntp.org"};
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
