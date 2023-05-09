package com.mayer;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListener;
import com.mayer.events.*;
import com.mayer.factory.NarcotrackEventHandler;
import com.mayer.frames.NarcotrackFrames;
import com.mayer.listeners.MariaDatabaseHandler;
import com.mayer.listeners.StatisticHandler;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;

public class NarcotrackListener implements SerialPortMessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(NarcotrackListener.class);
    private static final byte START_BYTE = (byte)0xFF;
    private static final byte END_BYTE = (byte)0xFE;

    private final ByteBuffer buffer;
    private final NarcotrackFrames shortestFrame;
    private final long startTimeLocal;
    private final long startTimeNTP;

    public NarcotrackListener() {
        LOGGER.info("starting...");

        startTimeLocal = System.currentTimeMillis();
        startTimeNTP = getNTPTime();
        if (startTimeNTP > 0) {
            LOGGER.debug("Got both times, difference is {}", startTimeLocal - startTimeNTP);
            if (Math.abs(startTimeLocal - startTimeNTP) > 10000) {
                LOGGER.error("Times differ more than 10s!");
            }
        }

        buffer = ByteBuffer.allocate(50000).order(ByteOrder.LITTLE_ENDIAN);
        shortestFrame = Arrays.stream(NarcotrackFrames.values()).min(Comparator.comparing(NarcotrackFrames::getLength)).orElse(NarcotrackFrames.ELECTRODE_CHECK);

        try {
            final MariaDatabaseHandler mariaDatabaseHandler = new MariaDatabaseHandler(this);
            final StatisticHandler statisticHandler = new StatisticHandler(startTimeLocal, startTimeNTP);
            EEGEvent.getEventHandlers().add(mariaDatabaseHandler);
            EEGEvent.getEventHandlers().add(statisticHandler);
            CurrentAssessmentEvent.getEventHandlers().add(mariaDatabaseHandler);
            CurrentAssessmentEvent.getEventHandlers().add(statisticHandler);
            PowerSpectrumEvent.getEventHandlers().add(mariaDatabaseHandler);
            PowerSpectrumEvent.getEventHandlers().add(statisticHandler);
            ElectrodeCheckEvent.getEventHandlers().add(mariaDatabaseHandler);
            ElectrodeCheckEvent.getEventHandlers().add(statisticHandler);
            RemainsEvent.getEventHandlers().add(mariaDatabaseHandler);
            RemainsEvent.getEventHandlers().add(statisticHandler);
        } catch (SQLException e) {
            LOGGER.error("Error adding frameListeners to NarcotrackListener", e);
            System.exit(1);
        }
    }

    @Override
    public byte[] getMessageDelimiter() {
        return new byte[] {END_BYTE};
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
        LOGGER.debug("SerialPortEvent (length: {}, buffer position: {})", serialPortEvent.getReceivedData().length, buffer.position());

        if(serialPortEvent.getReceivedData().length + buffer.position() > buffer.capacity()) {
            if (serialPortEvent.getReceivedData().length > buffer.capacity()) {
                LOGGER.error("Received more data than the buffer can hold (length: {}, buffer size: {}). Moving data and buffer to remains", serialPortEvent.getReceivedData().length, buffer.capacity());
                new RemainsEvent(getTimeDifference(), buffer);
                new RemainsEvent(getTimeDifference(), serialPortEvent.getReceivedData());
                return;
            } else {
                LOGGER.error("Buffer cannot hold received data (length of received data: {}, buffer position: {}, buffer capacity: {}). Clearing buffer and processing newly received data", serialPortEvent.getReceivedData().length, buffer.position(), buffer.capacity());
                new RemainsEvent(getTimeDifference(), buffer);
            }
        }

        buffer.put(serialPortEvent.getReceivedData());

        // Matching Package Types
        if (buffer.position() < shortestFrame.getLength()) {
            LOGGER.debug("Received fragment of {} bytes", buffer.position());
            return;
        }
 
        for (NarcotrackFrames frame : NarcotrackFrames.values()) {
            if (buffer.position() < frame.getLength()) continue;
            if (buffer.get(buffer.position() - frame.getLength() + 3) != frame.getIdentifier()) continue;
            if (buffer.get(buffer.position() - frame.getLength()) != START_BYTE) continue;

            LOGGER.debug("Found a frame of type {} in the buffer", frame);

            switch (frame) {
                case EEG:
                    new EEGEvent(getTimeDifference(), buffer);
                    break;
                case CURRENT_ASSESSMENT:
                    new CurrentAssessmentEvent(getTimeDifference(), buffer);
                    break;
                case POWER_SPECTRUM:
                    new PowerSpectrumEvent(getTimeDifference(), buffer);
                    break;
                case ELECTRODE_CHECK:
                    new ElectrodeCheckEvent(getTimeDifference(), buffer);
                    break;
            }

            if (buffer.position() > 0) {
                new RemainsEvent(getTimeDifference(), buffer);
            }

            return;
        }

        if (buffer.position() > 0) {
            LOGGER.warn("Received data than could not be matched to a handler. Buffer-Length is {}", buffer.position());
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
                    LOGGER.warn("Could not connect to ntp server {}", ntpHost);
                }
            }
            return 0;
        } catch (IOException e) {
            LOGGER.warn("Error creating NTPUDPClient", e);
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
