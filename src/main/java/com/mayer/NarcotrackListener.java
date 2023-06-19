package com.mayer;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListener;
import com.mayer.events.CurrentAssessmentEvent;
import com.mayer.events.EEGEvent;
import com.mayer.events.ElectrodeCheckEvent;
import com.mayer.events.PowerSpectrumEvent;
import com.mayer.events.RemainsEvent;
import com.mayer.listeners.ElectrodeDisconnectedListener;
import com.mayer.listeners.MariaDatabaseHandler;
import com.mayer.listeners.StatisticHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                Narcotrack.rebootPlatform();
            }
        }, checkSerialEventInterval, checkSerialEventInterval, TimeUnit.SECONDS);

        StatisticHandler statisticHandler = new StatisticHandler();
        MariaDatabaseHandler mariaDatabaseHandler = new MariaDatabaseHandler(this);
        ElectrodeDisconnectedListener electrodeDisconnectedListener = new ElectrodeDisconnectedListener();
        LOGGER.info("Initialization of NarcotrackListener completed");
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
    }

    private int getTimeDifference() {
        try {
            return Math.toIntExact(TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTimeReference, TimeUnit.NANOSECONDS));
        } catch(Exception e) {
            LOGGER.error("Could not getTimeDifference(). startTimeReference was {} (ns) and currentTime is {} (ns). currentTime is not 100% accurate, Exception Message: {}",System.nanoTime(), startTimeReference, e.getMessage(), e);
            return -1;
        }
    }

    public long getStartTime() {
        return startTime;
    }
}
