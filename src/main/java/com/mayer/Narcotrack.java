package com.mayer;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.mayer.events.*;
import com.mayer.listeners.ElectrodeDisconnectedListener;
import com.mayer.listeners.MariaDatabaseHandler;
import com.mayer.listeners.StatisticHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Narcotrack {

    private static final Logger LOGGER = LoggerFactory.getLogger(Narcotrack.class);

    private final String narcotrackSerialDescriptor = System.getenv("NARCOTRACK_SERIAL_DESCRIPTOR");
    private SerialPort serialPort;
    private final byte startByte = (byte)0xFF;
    private final byte endByte = (byte)0xFE;
    private final ByteBuffer buffer;
    private final NarcotrackFrames shortestFrame;
    private final long startTime;
    private final long startTimeReference;
    private int intervalsWithoutData = 0;

    private Narcotrack() {
        LOGGER.info("Application starting...");
        startTime = System.currentTimeMillis();
        startTimeReference = System.nanoTime();
        LOGGER.info("startTime is {}, startTimeReference is {}", startTime, startTimeReference);
        shortestFrame = Arrays.stream(NarcotrackFrames.values()).min(Comparator.comparing(NarcotrackFrames::getLength)).orElse(NarcotrackFrames.ELECTRODE_CHECK);
        buffer = ByteBuffer.allocate(50000).order(ByteOrder.LITTLE_ENDIAN);
        if (narcotrackSerialDescriptor == null || narcotrackSerialDescriptor.trim().isEmpty()) {
            LOGGER.error("Could not find serialPortDescriptor. Maybe, environment variables are not loaded?");
            rebootPlatform();
            return;
        }
        try {
            LOGGER.debug("Connecting to serial port using descriptor {}", narcotrackSerialDescriptor);
            serialPort = SerialPort.getCommPort(narcotrackSerialDescriptor);
            serialPort.setBaudRate(115200);
            serialPort.setNumDataBits(8);
            serialPort.setParity(SerialPort.NO_PARITY);
            serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
            serialPort.openPort();
            LOGGER.info("Connected to serial port");
        } catch (Exception e) {
            LOGGER.error("Could not connect to serial port, serialPortDescriptor: {}, Exception Message: {}", narcotrackSerialDescriptor, e.getMessage(), e);
            try {
                if (Arrays.stream(SerialPort.getCommPorts()).noneMatch(serialPort -> serialPort.getSystemPortPath().equalsIgnoreCase(narcotrackSerialDescriptor))) {
                    LOGGER.error("Port Descriptor {} does not match any of the available serial ports. Available ports are {}", narcotrackSerialDescriptor, Arrays.stream(SerialPort.getCommPorts()).map(sp -> sp.getSystemPortPath()).collect(Collectors.joining(" ")));
                } else {
                    LOGGER.error("Port Descriptor matches one of the available serial ports, but connection could not be opened");
                }
            } catch (Exception ex) {
                LOGGER.error("Could not create debug message showing CommPorts, Exception Message: {}", ex.getMessage(), ex);
            }
            rebootPlatform();
        }
        Runtime.getRuntime().addShutdownHook(new SerialPortShutdownHook());
        // JSerial only accepts ONE DataListener?!
        //serialPort.addDataListener(new SerialPortDisconnectListener());

        ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
        ScheduledFuture<?> scheduledFuture = ses.scheduleAtFixedRate(() -> {
            // Wartet scheduledFuture auf den vorherigen Durchlauf? Sonst könnte es eine Race Condition geben, wenn mal die DB timeoutet (Alternativ unwahrscheinlicher, wenn diese Aufgaben async ablaufen)
            int bytesAvailable = serialPort.bytesAvailable();
            if (bytesAvailable == 0) {
                intervalsWithoutData++;
                if (intervalsWithoutData%60 == 0) {
                    LOGGER.warn("No data received for {}mins", intervalsWithoutData);
                    if (intervalsWithoutData >= 300) {
                        Narcotrack.rebootPlatform();
                    }
                }
                return;
            }
            intervalsWithoutData = 0;

            if (bytesAvailable + buffer.position() > buffer.capacity()) {
                LOGGER.warn("bytesAvailable would overfill the remaining buffer space. Moving existing bytes in buffer to remains (bytesAvailable: {}, buffer.position(): {}, buffer.capacity(): {}).", bytesAvailable, buffer.position(), buffer.capacity());
                // Move Buffer to remains!
                new RemainsEvent(getTimeDifference(), buffer);
                if (bytesAvailable > buffer.capacity()) {
                    LOGGER.warn("bytesAvailable would overfill the entire buffer space. Moving bytesAvailable to remains");
                    // Move bytesAvailable to remains
                    byte[] data = new byte[bytesAvailable];
                    serialPort.readBytes(data, data.length);
                    new RemainsEvent(getTimeDifference(), data);
                    return;
                }
            }
            byte[] data = new byte[bytesAvailable];
            serialPort.readBytes(data, data.length); // Ist das überhaupt nötig?
            buffer.put(data);
            int lastPosition = buffer.position();
            buffer.position(0);
            for (int i = 0; i < lastPosition; i++) {
                if (buffer.get(i) == startByte) {
                    if (i + shortestFrame.getLength() - 1 > lastPosition) {
                        break;
                    }
                    if (i + 3 > lastPosition) { // < oder <= ?
                        continue;
                    }
                    for (NarcotrackFrames frame: NarcotrackFrames.values()) {
                        if (buffer.get(i + 3) != frame.getIdentifier()) {
                            continue;
                        }
                        if (i + frame.getLength() - 1 <= lastPosition && buffer.get(i + frame.getLength() - 1) == endByte) {
                            LOGGER.debug("Found a frame of type {} in the buffer", frame);
                            if (i > 0) {
                                LOGGER.warn("There is a fragment of {} bytes that cannot be categorized!", i);
                                byte[] remains = new byte[i];
                                buffer.position(0);
                                buffer.get(remains);
                                new RemainsEvent(getTimeDifference(), remains);
                            } else {
                                buffer.position(i);
                            }
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
                        }
                    }
                }
            }
        }, 1, 1, TimeUnit.SECONDS);

        StatisticHandler statisticHandler = new StatisticHandler();
        MariaDatabaseHandler mariaDatabaseHandler = new MariaDatabaseHandler(this);
        ElectrodeDisconnectedListener electrodeDisconnectedListener = new ElectrodeDisconnectedListener();
        LOGGER.info("Initialization of NarcotrackListener completed");
    }

    class SerialPortShutdownHook extends Thread {

        @Override
        public void run() {
            LOGGER.warn("Shutdown Hook triggered, closing serial port");
            if (serialPort != null && serialPort.isOpen()) {
                serialPort.closePort();
                LOGGER.info("Closed serial port");
            }
        }
    }

    class SerialPortDisconnectListener implements SerialPortDataListener {

        @Override
        public int getListeningEvents() {
            return SerialPort.LISTENING_EVENT_PORT_DISCONNECTED;
        }

        @Override
        public void serialEvent(SerialPortEvent event) {
            LOGGER.error("Listener noticed disconnect of serial port");
        }
    }

    public static void rebootPlatform() {
        LOGGER.error("Preparing reboot");
        if (System.getenv("PLEASE_DO_NOT_RESTART") != null) {
            LOGGER.info("Did not restart because of PLEASE_DO_NOT_RESTART");
            return;
        }
        try {
            Runtime.getRuntime().exec("sudo shutdown -r now");
        } catch (IOException e) {
            LOGGER.error("Cannot restart system. Error message: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        new Narcotrack();
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
