package com.mayer;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListener;
import com.mayer.event.frame.CurrentAssessmentEvent;
import com.mayer.event.frame.EEGEvent;
import com.mayer.event.frame.ElectrodeCheckEvent;
import com.mayer.event.frame.PowerSpectrumEvent;
import com.mayer.event.remains.RemainsEvent;
import com.mayer.frames.NarcotrackFrames;
import com.mayer.listeners.ElectrodeDisconnectedListener;
import com.mayer.listeners.MariaDatabaseHandler;
import com.mayer.listeners.StatisticHandler;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class NarcotrackListener implements SerialPortMessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(NarcotrackListener.class);

    private final byte START_BYTE = (byte)0xFF;
    private final byte END_BYTE = (byte)0xFE;
    private final ByteBuffer buffer;
    private final NarcotrackFrames shortestFrame;
    private final long startTime;
    private final long startTimeReference;
    private final int checkSerialEventInterval = 60;
    private int frameEventCount;
    private int intervalsWithoutData;

    public NarcotrackListener() {
        LOGGER.info("starting...");

        startTime = System.currentTimeMillis();
        startTimeReference = System.nanoTime();
        LOGGER.info("startTime is {}, startTimeReference is {}", startTime, startTimeReference);

        long startTimeNTP = getNTPTime();
        if (startTimeNTP > 0 && Math.abs((startTime / 1000000) - startTimeNTP) > 10000) {
            LOGGER.error("Difference between startTimeNTP and startTime is larger than 10s, battery probably empty");
        }

        buffer = ByteBuffer.allocate(50000).order(ByteOrder.LITTLE_ENDIAN);
        shortestFrame = Arrays.stream(NarcotrackFrames.values()).min(Comparator.comparing(NarcotrackFrames::getLength)).orElse(NarcotrackFrames.ELECTRODE_CHECK);

        frameEventCount = 0;
        intervalsWithoutData = 0;
        ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
        ScheduledFuture<?> scheduledFuture = ses.scheduleAtFixedRate(() -> {
            if (frameEventCount > 0) {
                frameEventCount = 0;
                intervalsWithoutData = 0;
                return;
            }
            intervalsWithoutData++;
            LOGGER.warn("No data received for {}mins", intervalsWithoutData);
            if (intervalsWithoutData >= 5) {
                LOGGER.error("Preparing reboot");
                // Hier könnte man Spaß haben mit System restarts
                if (System.getenv("PLEASE_DO_NOT_RESTART") != null) {
                    LOGGER.info("Did not restart because of PLEASE_DO_NOT_RESTART");
                    return;
                }

                try {
                    Runtime.getRuntime().exec("sudo shutdown -r now");
                } catch (IOException e) {
                    LOGGER.error("Cannot restart system");
                    System.exit(1);
                }
            }
        }, checkSerialEventInterval, checkSerialEventInterval, TimeUnit.SECONDS);

        StatisticHandler statisticHandler = new StatisticHandler();
        MariaDatabaseHandler mariaDatabaseHandler = new MariaDatabaseHandler(this);
        ElectrodeDisconnectedListener electrodeDisconnectedListener = new ElectrodeDisconnectedListener();
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
    public synchronized void serialEvent(SerialPortEvent serialPortEvent) {
        LOGGER.debug("SerialPortEvent (length: {}, buffer position: {})", serialPortEvent.getReceivedData().length, buffer.position());

        if(serialPortEvent.getReceivedData().length + buffer.position() > buffer.capacity()) {
            if (serialPortEvent.getReceivedData().length > buffer.capacity()) {
                LOGGER.warn("Received more data than the buffer can hold (length: {}, buffer size: {}). Moving data and buffer to remains", serialPortEvent.getReceivedData().length, buffer.capacity());
                new RemainsEvent(getTimeDifference(), buffer);
                new RemainsEvent(getTimeDifference(), serialPortEvent.getReceivedData());
                return;
            } else {
                LOGGER.warn("Buffer cannot hold received data (length of received data: {}, buffer position: {}, buffer capacity: {}). Clearing buffer and processing newly received data", serialPortEvent.getReceivedData().length, buffer.position(), buffer.capacity());
                new RemainsEvent(getTimeDifference(), buffer);
            }
        }

        buffer.put(serialPortEvent.getReceivedData());

        // Matching Package Types
        if (buffer.position() < shortestFrame.getLength()) {
            LOGGER.debug("Collected fragments of {} bytes", buffer.position());
            return;
        }
 
        for (NarcotrackFrames frame : NarcotrackFrames.values()) {
            if (buffer.position() < frame.getLength()) continue;
            if (buffer.get(buffer.position() - frame.getLength() + 3) != frame.getIdentifier()) continue;
            if (buffer.get(buffer.position() - frame.getLength()) != START_BYTE) continue;

            frameEventCount++;
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
            LOGGER.debug("Received data than could not be matched to a handler. Buffer-Length is {}", buffer.position());
        } else {
            LOGGER.debug("Does this ever trigger because of the return?");
        }
    }

    private long getNTPTime() {
        NTPUDPClient client = new NTPUDPClient();
        client.setDefaultTimeout(3000);
        try {
            client.open();
            client.setSoTimeout(3000);
            String[] ntpHosts = new String[]{"0.de.pool.ntp.org", "1.de.pool.ntp.org", "2.de.pool.ntp.org"};
            // String[] ntpHosts = new String[]{"0.de.pool.ntp.org"};
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
            LOGGER.error("Error creating NTPUDPClient", e);
            return 0;
        } finally {
            if (client.isOpen()) {
                client.close();
            }
        }
    }

    private int getTimeDifference() {
        try {
            return Math.toIntExact(TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTimeReference, TimeUnit.NANOSECONDS));
        } catch(Exception e) {
            LOGGER.error("Could not getTimeDifference(). startTimeReference was {} (ns) and currentTime is {} (ns). currentTime is not 100% accurate",System.nanoTime(), startTimeReference, e);
            return -1;
        }
    }

    public long getStartTime() {
        return startTime;
    }
}
