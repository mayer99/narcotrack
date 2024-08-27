package com.mayer99.narcotrack.data;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;
import com.mayer99.narcotrack.event.events.*;
import com.mayer99.narcotrack.application.NarcotrackApplication;
import com.mayer99.narcotrack.event.NarcotrackEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class NarcotrackSerialPortReader {

    private final Logger LOGGER = LoggerFactory.getLogger(NarcotrackSerialPortReader.class);

    private final byte START_BYTE = (byte)0xFF;
    private final byte END_BYTE = (byte)0xFE;

    private final NarcotrackApplication application;
    private final NarcotrackEventManager eventManager;
    private SerialPort serialPort;
    private final ByteBuffer buffer = ByteBuffer.allocate(200_000).order(ByteOrder.LITTLE_ENDIAN);
    private final ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
    private final ScheduledFuture<?> serialReadoutTask;
    private int intervalsWithoutData = 0;
    private boolean isRecording = false;
    private Instant recordingStartTime;

    public NarcotrackSerialPortReader(NarcotrackApplication application) {
        this.application = application;
        eventManager = application.getEventManager();
        initializeSerialPort();
        serialReadoutTask = ses.scheduleAtFixedRate(this::onInterval, 1, 1, TimeUnit.SECONDS);
    }

    private void logSerialPortDescriptors() {
        try {
            LOGGER.info("SerialPortDescriptors are {}", Arrays.stream(SerialPort.getCommPorts()).map(SerialPort::getSystemPortPath).collect(Collectors.joining(", ")));
        } catch (Exception e) {
            LOGGER.error("Could not list SerialPortDescriptors", e);
        }
    }

    private void initializeSerialPort() {
        try {
            String serialPortDescriptor = NarcotrackApplication.getEnvironmentVariable("NARCOTREND_PORT");
            LOGGER.info("Connecting to SerialPort using descriptor {}...", serialPortDescriptor);
            serialPort = SerialPort.getCommPort(serialPortDescriptor);
            serialPort.setComPortParameters(115200, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
            if (!serialPort.openPort()) {
                throw new RuntimeException("Could not open serial port");
            }
            LOGGER.info("Connected to SerialPort");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize SerialPort.", e);
            logSerialPortDescriptors();
            eventManager.dispatchOnRecoverableError();
            application.scheduleRestart();
            application.cleanupAndExit();
        }

    }

    public void cleanup() {
        if (serialPort != null && serialPort.isOpen()) {
            LOGGER.info("Attempting to close SerialPort");
            if (serialPort.closePort()) {
                LOGGER.info("Closed SerialPort");
            } else {
                LOGGER.error("Could not close SerialPort");
            }
        }
        if (serialReadoutTask != null) {
            serialReadoutTask.cancel(true);
        }
        ses.shutdown();
    }

    private void handleUnpluggedSerialCable() {
        LOGGER.error("Serial cable seems to be unplugged");
        if (isRecording) stopRecording();
        eventManager.dispatchOnRecoverableError();
        application.scheduleRestart();
        application.cleanupAndExit();
    }

    private void onInterval() {
        LOGGER.debug("SES called onInterval");
        eventManager.dispatchOnIntervalStart();
        processSerialData();
        eventManager.dispatchOnIntervalStop();
    }

    private void processSerialData() {
        try {
            int bytesAvailable = serialPort.bytesAvailable();
            if (bytesAvailable < 0) handleUnpluggedSerialCable();
            if (bytesAvailable == 0) {
                if (isRecording) {
                    LOGGER.info("Did not receive any data in this interval. Stopping recording");
                    stopRecording();
                }
                intervalsWithoutData++;
                if (intervalsWithoutData%300 == 0) {
                    intervalsWithoutData = 0;
                    LOGGER.debug("No data received for 5 min");
                }
                return;
            }

            if (!isRecording) {
                intervalsWithoutData = 0;
                LOGGER.info("Starting new recording");
                startRecording();
            }
            int time = (int) (Duration.between(recordingStartTime, Instant.now()).abs().toSeconds());
            byte[] data = new byte[bytesAvailable];
            serialPort.readBytes(data, data.length);
            eventManager.dispatchOnReceivedData(data);

            LOGGER.debug("Buffer contains {}/{} bytes. Data contains {} bytes", buffer.position(), buffer.capacity(), data.length);
            if (data.length > buffer.remaining()) {
                if (data.length > buffer.capacity()) {
                    LOGGER.warn("Even the new data would overflow the buffer");
                    byte[] remains = new byte[buffer.position() + data.length];
                    buffer.flip();
                    buffer.get(remains, 0, buffer.position());
                    System.arraycopy(data, 0, remains, buffer.position(), data.length);
                    buffer.clear();
                    eventManager.dispatchOnHandleRemains(new ReceivedRemainsEvent(time, remains));
                    return;
                }
                LOGGER.warn("New data would overflow remaining buffer space");
                byte[] remains = new byte[buffer.position()];
                buffer.flip();
                buffer.get(remains);
                buffer.clear();
                eventManager.dispatchOnHandleRemains(new ReceivedRemainsEvent(time, remains));
            }
            buffer.put(data);
            buffer.flip();
            for(int i = 0; i < buffer.limit(); i++) {
                if (buffer.get(i) != START_BYTE) continue;
                for (NarcotrackFrame frame: NarcotrackFrame.values()) {
                    if (buffer.limit() - i < frame.getLength()) continue;
                    if (buffer.get(i + 3) != frame.getIdentifier()) continue;
                    if (buffer.get(i + frame.getLength() - 1) != END_BYTE) continue;
                    if (buffer.position() != i) {
                        byte[] remains = new byte[i - buffer.position()];
                        buffer.get(remains);
                        LOGGER.warn("There are remains before frame of type {}", frame);
                        eventManager.dispatchOnHandleRemains(new ReceivedRemainsEvent(time, remains));
                    }
                    buffer.position(i);
                    buffer.mark();
                    switch (frame) {
                        case EEG -> eventManager.dispatchOnReceivedEEG(new ReceivedEEGEvent(time, buffer));
                        case CURRENT_ASSESSMENT -> eventManager.dispatchOnReceivedCurrentAssessment(new ReceivedCurrentAssessmentEvent(time, buffer));
                        case POWER_SPECTRUM -> eventManager.dispatchOnReceivedPowerSpectrum(new ReceivedPowerSpectrumEvent(time, buffer));
                        case ELECTRODE_CHECK -> eventManager.dispatchOnReceivedElectrodeCheck(new ReceivedElectrodeCheckEvent(time, buffer));
                    }
                    buffer.position(i + frame.getLength());
                    i += frame.getLength() - 1;
                    break;
                }
            }
            if (buffer.hasRemaining()) {
                LOGGER.debug("Remaining bytes of length {} at the end of the buffer", buffer.remaining());
                buffer.compact();
            } else {
                buffer.clear();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to process serial data", e);
            eventManager.dispatchOnRecoverableError();
            application.scheduleRestart();
            application.cleanupAndExit();
        }
    }

    private void startRecording() {
        isRecording = true;
        recordingStartTime = Instant.now();
        eventManager.dispatchOnRecordingStart(recordingStartTime);
    }

    private void stopRecording() {
        isRecording = false;
        recordingStartTime = null;
        eventManager.dispatchOnRecordingStop();
    }


}
