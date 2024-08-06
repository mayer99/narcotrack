package com.mayer99;

import com.fazecast.jSerialComm.SerialPort;
import com.mayer99.logging.SocketAppender;
import com.mayer99.narcotrack.base.events.*;
import com.mayer99.narcotrack.base.models.NarcotrackEventHandler;
import com.mayer99.narcotrack.base.models.NarcotrackFrameType;
import com.mayer99.narcotrack.handlers.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Narcotrack {

    private static final String SERIAL_PORT_DESCRIPTOR = "NT_SERIAL_PORT_DESCRIPTOR";

    private static final byte START_BYTE = (byte)0xFF;
    private static final byte END_BYTE = (byte)0xFE;

    private static final Logger logger = LoggerFactory.getLogger(Narcotrack.class);
    private static final ArrayList<NarcotrackEventHandler> handlers = new ArrayList<>();


    private final ByteBuffer buffer = ByteBuffer.allocate(200_000).order(ByteOrder.LITTLE_ENDIAN);
    private SerialPort serialPort;
    private ScheduledFuture<?> serialReadoutTask;
    private int intervalsWithoutData = 0;
    private boolean isRecording = false;
    private Instant startTime;

    private Narcotrack() {
        logger.info("Application starting...");
        if (SocketAppender.disabled) {
            logger.warn("SocketAppender is disabled");
        }

        handlers.add(new StatusLightHandler());
        handlers.add(new CriticalErrorHandler());
        handlers.add(new BackupHandler());
        handlers.add(new BadElectrodeListener());
        handlers.add(new StatisticsHandler());
        try {
            handlers.add(new MariaDatabaseHandler());
        } catch (Exception e) {
            logger.error("Could not start MariaDatabaseHandler");
            Narcotrack.getHandlers().forEach(NarcotrackEventHandler::onCriticalError);
        }
        try {
            initializeSerialPort();
        } catch (Exception e) {
            logger.error("Could not connect to serial port", e);
            try {
                logger.info("Available serial ports are {}", Arrays.stream(SerialPort.getCommPorts()).map(SerialPort::getSystemPortPath).collect(Collectors.joining(", ")));
            } catch (Exception ex) {
                logger.error("Could not create debug message showing CommPorts", ex);
            }
            Narcotrack.getHandlers().forEach(NarcotrackEventHandler::onCriticalError);
        }
        scheduleSerialPortReadout();
    }

    private void initializeSerialPort() throws Exception {
        String descriptor = System.getenv(SERIAL_PORT_DESCRIPTOR);
        if (Narcotrack.isNullEmptyOrWhitespace(descriptor)) {
            throw new IllegalArgumentException(String.format("The environment variable %s ist not set or has an invalid value. Are the environment variables loaded?", SERIAL_PORT_DESCRIPTOR));
        }
        logger.info("Connecting to serial port using descriptor {}", descriptor);
        serialPort = SerialPort.getCommPort(descriptor);
        serialPort.setBaudRate(115200);
        serialPort.setNumDataBits(8);
        serialPort.setParity(SerialPort.NO_PARITY);
        serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
        serialPort.openPort();
        Runtime.getRuntime().addShutdownHook(new SerialPortShutdownHook());
        logger.info("Connected to serial port");
    }

    class SerialPortShutdownHook extends Thread {
        @Override
        public void run() {
            logger.warn("SerialPortShutdownHook triggered");
            if (serialPort == null || !serialPort.isOpen()) {
                logger.info("SerialPort is null or closed");
                return;
            }
            logger.info("Attempting to close SerialPort");
            try {
                serialPort.closePort();
                logger.info("Closed serialPort");
            } catch (Exception e) {
                logger.error("Could not close SerialPort", e);
            }
        }
    }

    private void scheduleSerialPortReadout() {
        ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
        serialReadoutTask = ses.scheduleAtFixedRate(this::readSerialData, 1, 1, TimeUnit.SECONDS);
    }

    private synchronized void readSerialData() {
        logger.debug("Tick");
        int bytesAvailable = serialPort.bytesAvailable();
        if (bytesAvailable < 0) {
            logger.error("SerialPort returns -1 bytes, there is probably an unplugged cable");
            if (isRecording) {
                isRecording = false;
                startTime = null;
                handlers.forEach(NarcotrackEventHandler::onRecordingStop);
            }
            Narcotrack.getHandlers().forEach(NarcotrackEventHandler::onCriticalError);
            if (serialReadoutTask != null) {
                logger.info("SerialRedoutTask is not null, trying to canacel task without interrupting");
                serialReadoutTask.cancel(false);
            } else {
                logger.error("SerialRedoutTask is null, could not interrupt");
            }
            return;
        }
        if (bytesAvailable == 0) {
            intervalsWithoutData++;
            if (isRecording && intervalsWithoutData >= 3) {
                logger.info("Stopped recording");
                isRecording = false;
                startTime = null;
                logger.debug("Test3");
                handlers.forEach(NarcotrackEventHandler::onRecordingStop);
                logger.debug("Test4");
            }
            if (intervalsWithoutData%300 == 0) {
                intervalsWithoutData = 0;
                logger.info("No data received for 5 mins");
            }
            return;
        }
        intervalsWithoutData = 0;

        if (!isRecording) {
            logger.info("Starting new recording");
            isRecording = true;
            startTime = Instant.now();
            handlers.forEach(handler -> handler.onRecordingStart(startTime));
        }
        int time = (int) (Duration.between(startTime, Instant.now()).abs().toSeconds());
        byte[] data = new byte[bytesAvailable];
        serialPort.readBytes(data, data.length);
        handlers.forEach(handler -> handler.onBackup(data));

        if (data.length + buffer.position() > buffer.capacity()) {
            logger.warn("Buffer contains {}/{} bytes", buffer.position(), buffer.capacity());
            logger.warn("New data of length {} would overflow remaining buffer space, moving contents to buffer", data.length);
            byte[] bufferData = new byte[buffer.position()];
            buffer.get(bufferData);
            buffer.clear();
            new RemainsEvent(time, bufferData);
            if (data.length > buffer.capacity()) {
                logger.warn("New data of length {} would overflow buffer, offloading new data immediately", data.length);
                new RemainsEvent(time, data);
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
            if (i + 6 >= lastPosition) continue;
            for (NarcotrackFrameType frame: NarcotrackFrameType.values()) {
                if (buffer.get(i + 3) != frame.getIdentifier()) continue;
                if (i + frame.getLength() > lastPosition) continue;
                if (buffer.get(i + frame.getLength() - 1) != END_BYTE) continue;
                frame.count();
                if (buffer.position() != i) {
                    byte[] remains = new byte[i - buffer.position()];
                    buffer.get(remains);
                    logger.warn("There are remains before frame of type {} number {}", frame, frame.getCount());
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
            logger.debug("endFragment of length {} found, adding to the beginning", endFragment.length);
            buffer.get(endFragment);
            buffer.clear();
            buffer.put(endFragment);
        } else {
            buffer.clear();
        }
        Narcotrack.getHandlers().forEach(NarcotrackEventHandler::onEndOfInterval);
    }

    public static void main(String[] args) {
        new Narcotrack();
    }

    public static ArrayList<NarcotrackEventHandler> getHandlers() {
        return handlers;
    }

    public static void rebootPlatform() {
        logger.error("Reboot ################################################################################");
        if (System.getenv("PLEASE_DO_NOT_RESTART") == null) {
            try {
                Runtime.getRuntime().exec("sudo shutdown -r now");
            } catch (IOException e) {
                logger.error("Cannot restart system", e);
            }
        } else {
            logger.info("Did not restart because of PLEASE_DO_NOT_RESTART");
        }
        System.exit(1);
    }

    public static boolean isNullEmptyOrWhitespace(String text) {
        return text == null || text.trim().isEmpty();
    }

}