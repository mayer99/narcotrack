package com.mayer99.narcotrack.base.handler;

import com.fazecast.jSerialComm.SerialPort;
import com.mayer99.Narcotrack;
import com.mayer99.lights.StatusLightController;
import com.mayer99.lights.enums.StatusLight;
import com.mayer99.lights.enums.StatusLightColor;
import com.mayer99.lights.models.StatusLightAnimation;
import com.mayer99.narcotrack.base.models.NarcotrackEventHandler;
import com.mayer99.narcotrack.base.models.NarcotrackFrameType;
import com.mayer99.narcotrack.base.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SerialPortHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SerialPortHandler.class);
    private static final byte START_BYTE = (byte)0xFF;
    private static final byte END_BYTE = (byte)0xFE;
    private static final int MAX_BACKUP_DATA_BATCHES = 30;

    private final Instant startTime;
    private final ByteBuffer buffer;
    private final ByteBuffer backupDataBuffer;
    private final Path backupFilePath;
    private final StatusLightController statusLights;
    private SerialPort serialPort;
    private int backupDataCounter = 0;
    private int intervalsWithoutData = 0;


    public SerialPortHandler(Narcotrack narcotrack) {
        startTime = narcotrack.getStartTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String backupFileName = sdf.format(Date.from(startTime)) + ".bin";
        backupFilePath = Paths.get("backups", backupFileName);
        LOGGER.info("Backup file is called {}", backupFileName);

        buffer = ByteBuffer.allocate(50_000).order(ByteOrder.LITTLE_ENDIAN);
        backupDataBuffer = ByteBuffer.allocate(100_000).order(ByteOrder.LITTLE_ENDIAN);

        statusLights = narcotrack.getStatusLights();
        if (!initializeSerialPort()) Narcotrack.rebootPlatform();

        ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
        ses.scheduleAtFixedRate(this::readSerialData, 1, 1, TimeUnit.SECONDS);
    }

    private boolean initializeSerialPort() {
        String serialDescriptor = System.getenv("SERIAL_DESCRIPTOR");
        if (serialDescriptor == null || serialDescriptor.trim().isEmpty()) {
            LOGGER.error("Could not find serialDescriptor. Maybe, environment variables are not loaded?");
            return false;
        }
        try {
            LOGGER.debug("Connecting to serial port using serialDescriptor {}", serialDescriptor);
            serialPort = SerialPort.getCommPort(serialDescriptor);
            serialPort.setBaudRate(115200);
            serialPort.setNumDataBits(8);
            serialPort.setParity(SerialPort.NO_PARITY);
            serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
            serialPort.openPort();
            Runtime.getRuntime().addShutdownHook(new SerialPortShutdownHook());
            LOGGER.info("Connected to serial port");
            return true;
        } catch (Exception e) {
            LOGGER.error("Could not connect to serial port", e);
            try {
                LOGGER.error("Available serial ports are {}", Arrays.stream(SerialPort.getCommPorts()).map(SerialPort::getSystemPortPath).collect(Collectors.joining(", ")));
            } catch (Exception ex) {
                LOGGER.error("Could not create debug message showing CommPorts", ex);
            }
            return false;
        }
    }

    class SerialPortShutdownHook extends Thread {
        @Override
        public void run() {
            LOGGER.warn("SerialPortShutdownHook triggered");
            if (serialPort != null && serialPort.isOpen()) {
                LOGGER.info("Closing serialPort");
                serialPort.closePort();
                LOGGER.info("Closed serialPort");
            }
        }
    }

    private synchronized void readSerialData() {
        int bytesAvailable = serialPort.bytesAvailable();
        LOGGER.debug("{} bytes available", bytesAvailable);
        if (bytesAvailable <= 0) {
            intervalsWithoutData++;
            if (intervalsWithoutData%60 == 0) {
                int minutesWithoutData = intervalsWithoutData/60;
                LOGGER.warn("No data received for {} {}", minutesWithoutData, minutesWithoutData == 1 ? "min" : "mins");
                if (minutesWithoutData >= 5) Narcotrack.rebootPlatform();
            }
            statusLights.animate(new StatusLightAnimation(StatusLight.STATUS, intervalsWithoutData >= 180 ? StatusLightColor.ERROR : StatusLightColor.WARNING));
            Narcotrack.getHandlers().forEach(NarcotrackEventHandler::onEndOfInterval);
            return;
        }
        intervalsWithoutData = 0;

        int time;
        try {
            time = Math.toIntExact(Duration.between(startTime, Instant.now()).getSeconds());
        } catch (ArithmeticException e) {
            LOGGER.error("Could not cast time duration to int. startTime was: {}", startTime, e);
            time = -1;
        }
        byte[] data = new byte[bytesAvailable];
        serialPort.readBytes(data, data.length);
        addToBackupBuffer(data);


        if (data.length + buffer.position() > buffer.capacity()) {
            LOGGER.warn("bytesAvailable would overfill the remaining buffer space. Moving existing bytes in buffer to remains (bytesAvailable: {}, buffer.position(): {}, buffer.capacity(): {}).", bytesAvailable, buffer.position(), buffer.capacity());
            byte[] bufferData = new byte[buffer.position()];
            buffer.get(bufferData);
            buffer.clear();
            new RemainsEvent(time, bufferData);
            if (data.length > buffer.capacity()) {
                LOGGER.warn("bytesAvailable would overfill the entire buffer space. Moving bytesAvailable to remains");
                new RemainsEvent(time, data);
                statusLights.animate(new StatusLightAnimation(StatusLight.STATUS, StatusLightColor.WARNING));
                Narcotrack.getHandlers().forEach(NarcotrackEventHandler::onEndOfInterval);
                return;
            }
        }
        buffer.put(data);
        int lastPosition = buffer.position();
        buffer.position(0);
        for(int i = 0; i < lastPosition; i++) {
            if (i + NarcotrackFrameType.SHORTEST_FRAME_TYPE.getLength() > lastPosition) break;
            if (buffer.get(i) != START_BYTE) continue;
            if (i + 6 >= lastPosition) continue; // length, identifier, chkSum and tail
            for (NarcotrackFrameType frame: NarcotrackFrameType.values()) {
                if (buffer.get(i + 3) != frame.getIdentifier()) continue;
                if (i + frame.getLength() > lastPosition) continue;
                if (buffer.get(i + frame.getLength() - 1) != END_BYTE) continue;
                //LOGGER.debug("Found a frame of type {} in the buffer", frame);
                frame.count();
                if (buffer.position() != i) {
                    byte[] remains = new byte[i - buffer.position()];
                    buffer.get(remains);
                    LOGGER.warn("There are remains before frame of type {} number {}", frame, frame.getCount());
                    new RemainsEvent(time, remains);
                }
                buffer.position(i);
                switch (frame) {
                    case EEG -> new EEGEvent(time, buffer);
                    case CURRENT_ASSESSMENT -> new CurrentAssessmentEvent(time, buffer);
                    case POWER_SPECTRUM -> new PowerSpectrumEvent(time, buffer);
                    case ELECTRODE_CHECK -> new ElectrodeCheckEvent(time, buffer);
                }
                buffer.position(i + frame.getLength());
                i += frame.getLength() - 1;
                break;
            }
        }
        if (lastPosition > buffer.position()) {
            byte[] endFragment = new byte[lastPosition - buffer.position()];
            LOGGER.debug("endFragment of length {} found, adding to the beginning", endFragment.length);
            buffer.get(endFragment);
            buffer.clear();
            buffer.put(endFragment);
        } else {
            buffer.clear();
        }
        statusLights.animate(new StatusLightAnimation(StatusLight.STATUS, StatusLightColor.INFO));
        Narcotrack.getHandlers().forEach(NarcotrackEventHandler::onEndOfInterval);
    }

    private void addToBackupBuffer(byte[] data) {
        if (data.length + backupDataBuffer.position() > backupDataBuffer.capacity()) {
            backupDataCounter = 0;
            LOGGER.warn("Adding new bytes to backupBuffer would cause an overflow. Moving contents of the buffer to the backup file");
            byte[] bufferData = new byte[backupDataBuffer.position()];
            backupDataBuffer.position(0);
            backupDataBuffer.get(bufferData);
            backupDataBuffer.clear();
            saveToBackupFile(bufferData);
            if (data.length > backupDataBuffer.capacity()) {
                LOGGER.warn("New bytes are longer than the buffer size. Moving new data to the backup file");
                saveToBackupFile(data);
                return;
            }

        }
        backupDataCounter++;
        backupDataBuffer.put(data);
        LOGGER.debug("Added {} bytes to backupDataBuffer", data.length);
        if (backupDataCounter >= MAX_BACKUP_DATA_BATCHES) {
            backupDataCounter = 0;
            byte[] backupData = new byte[backupDataBuffer.position()];
            backupDataBuffer.position(0);
            backupDataBuffer.get(backupData);
            backupDataBuffer.clear();
            saveToBackupFile(backupData);
        }
    }

    private void saveToBackupFile(byte[] data) {
        try {
            Files.write(backupFilePath, data, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            LOGGER.info("Added {} bytes to the backup file", data.length);
        } catch (IOException e) {
            LOGGER.error("saveToBackupFile failed", e);
        }
    }

}
