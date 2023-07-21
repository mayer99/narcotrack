package com.mayer;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.mayer.events.*;
import com.mayer.listeners.EEGMonitorHandler;
import com.mayer.listeners.ElectrodeDisconnectedListener;
import com.mayer.listeners.MariaDatabaseHandler;
import com.mayer.listeners.StatisticsHandler;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Narcotrack {

    private static final Logger LOGGER = LoggerFactory.getLogger(Narcotrack.class);
    private static final byte START_BYTE = (byte)0xFF;
    private static final byte END_BYTE = (byte)0xFE;
    private static final ArrayList<NarcotrackEventHandler> HANDLERS = new ArrayList<>();

    private SerialPort serialPort;
    private final ByteBuffer buffer;
    private final Instant startTime;
    private final long startTimeReference;
    private final String backupFileName;
    private final Path backupFilePath;
    private int backupDataCounter = 0;
    private ByteBuffer backupDataBuffer;
    private int intervalsWithoutData = 0;

    private Narcotrack() {
        LOGGER.info("Application starting...");
        startTime = Instant.now();
        startTimeReference = System.nanoTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        backupFileName = sdf.format(Date.from(startTime)) + ".bin";
        backupFilePath = Paths.get("backup", backupFileName);
        LOGGER.info("startTime: {}, startTimeReference: {}, backupFile: {}", startTime.getEpochSecond(), startTimeReference, backupFileName);
        buffer = ByteBuffer.allocate(50000).order(ByteOrder.LITTLE_ENDIAN);
        backupDataBuffer = ByteBuffer.allocate(100000).order(ByteOrder.LITTLE_ENDIAN);

        if (!initializeSerialPort()) {
            LOGGER.error("Rebooting because of error initializing serial port");
            rebootPlatform();
            return;
        }
        scheduleSerialPortReadout();

        MariaDatabaseHandler mariaDatabaseHandler = new MariaDatabaseHandler(this);
        ElectrodeDisconnectedListener electrodeDisconnectedListener = new ElectrodeDisconnectedListener();
        EEGMonitorHandler eegMonitorHandler = new EEGMonitorHandler();
        StatisticsHandler statisticsHandler = new StatisticsHandler();
    }

    class SerialPortShutdownHook extends Thread {

        @Override
        public void run() {
            LOGGER.warn("Shutdown Hook triggered, closing serial port");
            if (serialPort != null && serialPort.isOpen()) {
                serialPort.closePort();
                LOGGER.info("Closed serial port ");
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
            rebootPlatform();
        }
    }

    public static void rebootPlatform() {
        LOGGER.error("Preparing reboot ##########################################################################################");
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

    public Instant getStartTime() {
        return startTime;
    }

    private boolean initializeSerialPort() {
        String SERIAL_DESCRIPTOR = System.getenv("NARCOTRACK_SERIAL_DESCRIPTOR");
        if (SERIAL_DESCRIPTOR == null || SERIAL_DESCRIPTOR.trim().isEmpty()) {
            LOGGER.error("Could not find NARCOTRACK_SERIAL_DESCRIPTOR. Maybe, environment variables are not loaded?");
            return false;
        }
        try {
            LOGGER.debug("Connecting to serial port using descriptor {}", SERIAL_DESCRIPTOR);
            serialPort = SerialPort.getCommPort(SERIAL_DESCRIPTOR);
            serialPort.setBaudRate(115200);
            serialPort.setNumDataBits(8);
            serialPort.setParity(SerialPort.NO_PARITY);
            serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
            serialPort.openPort();
            Runtime.getRuntime().addShutdownHook(new SerialPortShutdownHook());
            LOGGER.info("Connected to serial port");
            return true;
        } catch (Exception e) {
            LOGGER.error("Could not connect to serial port, SERIAL_DESCRIPTOR: {}, Exception Message: {}", SERIAL_DESCRIPTOR, e.getMessage(), e);
            try {
                if (Arrays.stream(SerialPort.getCommPorts()).noneMatch(serialPort -> serialPort.getSystemPortPath().equalsIgnoreCase(SERIAL_DESCRIPTOR))) {
                    LOGGER.error("Port Descriptor {} does not match any of the available serial ports. Available ports are {}", SERIAL_DESCRIPTOR, Arrays.stream(SerialPort.getCommPorts()).map(sp -> sp.getSystemPortPath()).collect(Collectors.joining(" ")));
                } else {
                    LOGGER.error("Port Descriptor matches one of the available serial ports, but connection could not be opened");
                }
            } catch (Exception ex) {
                LOGGER.error("Could not create debug message showing CommPorts, Exception Message: {}", ex.getMessage(), ex);
            }
            return false;
        }
    }

    private void scheduleSerialPortReadout() {
        ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
        ScheduledFuture<?> scheduledFuture = ses.scheduleAtFixedRate(() -> {
            // Wartet scheduledFuture auf den vorherigen Durchlauf? Sonst könnte es eine Race Condition geben, wenn mal die DB timeoutet (Alternativ unwahrscheinlicher, wenn diese Aufgaben async ablaufen)
            int bytesAvailable = serialPort.bytesAvailable();
            LOGGER.debug("{} bytes available", bytesAvailable);
            if (bytesAvailable > 0) {
                intervalsWithoutData++;
                if (intervalsWithoutData%60 == 0) {
                    LOGGER.warn("No data received for {}mins", intervalsWithoutData/60);
                    if (intervalsWithoutData/60 >= 3) {
                        Narcotrack.rebootPlatform();
                    }
                }
                return;
            }
            intervalsWithoutData = 0;
            int time;
            try {
                time = Math.toIntExact(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTimeReference));
            } catch(Exception e) {
                LOGGER.error("Could not calculate timeDifference. startTimeReference was {} (ns) and currentTime is {} (ns). currentTime is not 100% accurate, Exception Message: {}",System.nanoTime(), startTimeReference, e.getMessage(), e);
                time = -1;
            }

            if (bytesAvailable + buffer.position() > buffer.capacity()) {
                // buffer.position() ist 1 größer als der tatsächliche Füllzustand des Buffers. buffer.capacity() entspricht exakt der Größe.
                // Angenommen cap wäre 10, enthalten sind 5 (dadurch buffer.position() -> 5) und 5 bytesAvailable => 10 -> nicht größer als capacity() (10), daher okay
                // Angenommen cap wäre 10, enthalten sind 5 (dadurch buffer.position() -> 5) und 6 bytesAvailable => 11 > 10 -> abgelehnt
                LOGGER.warn("bytesAvailable would overfill the remaining buffer space. Moving existing bytes in buffer to remains (bytesAvailable: {}, buffer.position(): {}, buffer.capacity(): {}).", bytesAvailable, buffer.position(), buffer.capacity());
                // Move Buffer to remains!
                new RemainsEvent(time, buffer);
                buffer.clear();
                if (bytesAvailable > buffer.capacity()) {
                    LOGGER.warn("bytesAvailable would overfill the entire buffer space. Moving bytesAvailable to remains");
                    // Move bytesAvailable to remains
                    byte[] data = new byte[bytesAvailable];
                    serialPort.readBytes(data, data.length);
                    saveBytesToBackupFile(data);
                    new RemainsEvent(time, data);
                    return;
                }
            }
            byte[] data = new byte[bytesAvailable];
            serialPort.readBytes(data, data.length);
            saveBytesToBackupFile(data);
            buffer.put(data);
            int lastPosition = buffer.position();
            buffer.position(0);
            for(int i = 0; i < lastPosition; i++) {
                // lastPosition ist 5, da 5 bytes gespeichert sind [0, 4]
                // i braucht byte 5 nicht anfragen, da er nicht relevant ist
                // i < lastPosition statt <=, da sonst ein irrelevanter Index mitgenommen wird
                if (i + NarcotrackFrameType.SHORTEST_FRAME_TYPE.getLength() > lastPosition) {
                    // Wenn buffer mit 12 bytes gefüllt -> buffer.position(): 12; shortestFrame: 10; i = 0; i++
                    // i: 0; 10 >! 12; verfügbare Bytes: 12
                    // i: 1; 11 >! 12; verfügbare Bytes: 11
                    // i: 2; 12 >! 12; verfügbare Bytes: 10
                    // i: 3; 13 > 12; verfügbare Bytes: 9
                    break;
                }
                if (buffer.get(i) == START_BYTE) {
                    if (i + 3 >= lastPosition) {
                        // buffer enthält 4 bytes [0, 3]; buffer.position()/lastPosition: 4;
                        // i: 0 ; 3 >= 4 => fail
                        // i: 1; 4 >= 4 => ok
                        continue;
                    }
                    for (NarcotrackFrameType frame: NarcotrackFrameType.values()) {
                        if (buffer.get(i + 3) != frame.getIdentifier()) {
                            continue;
                        }
                        if (i + frame.getLength() > lastPosition) {
                            // Bsp.: frame 5 Bytes; buffer hat 5 bytes: buffer.position()/lastPosition: 5
                            // i: 0 =>
                            continue;
                        }
                        if (buffer.get(i + frame.getLength() - 1) != END_BYTE) {
                            continue;
                        }
                        LOGGER.debug("Found a frame of type {} in the buffer", frame);
                        frame.count();
                        // Wenn zwischen zwei Paketen ein defektes Paket oder Fragment liegt, wird es nicht gespeichert.
                        if (buffer.position() != i) {
                            // buffer.position(): 0; i: 2 => byte[0, 1] ist remains
                            // buffer.position: 3; i: 6 => byte[3, 5] ist remains
                            byte[] remains = new byte[i - buffer.position()];
                            buffer.get(remains);
                            LOGGER.warn("There are reamins before frame of type {} number {}", frame, frame.getCount());
                            new RemainsEvent(time, remains);
                        }
                        buffer.position(i);
                        switch (frame) {
                            case EEG:
                                new EEGEvent(time, buffer);
                                break;
                            case CURRENT_ASSESSMENT:
                                new CurrentAssessmentEvent(time, buffer);
                                break;
                            case POWER_SPECTRUM:
                                new PowerSpectrumEvent(time, buffer);
                                break;
                            case ELECTRODE_CHECK:
                                new ElectrodeCheckEvent(time, buffer);
                                break;
                        }
                        // Paket der Länge 5 beginnt bei 0 (byte[0,4]) und wird nur von [0,2] gelesen. Soll eigentlich bei
                        // buffer.position(): 5 sein (vor dem 5. Byte) entsprechend der Paketlänge
                        // Korrektur daher auf i + Paketlänge
                        buffer.position(i + frame.getLength());

                        // buffer.position(): 3 (da byte[0,2] als Paket abgearbeitet wurden), i: 0
                        // i += 3 - 1, damit im nächsten Durchlauf mit i++ i: 3 entspricht (wäre dann auch der nächste Start-Byte, wenn das letzte Paket bis 2 ging)
                        i += frame.getLength() - 1;
                        break;
                        // break nötig, da sonst innerhalb des Pakets ein weiteres Paket gefunden werden könnte. Wenn das passiert, wäre aber der buffer komplett an der falschen Position, da er in der Loop nicht deklarativ gesetzt, sondern als Marker genutzt wird
                    }
                }
            }
            if (lastPosition > buffer.position()) {
                // lastPosition: 7 für 7 bytes, buffer.position auf 5 [0,4] gesammelt
                // 2 byte fehlen => 7 - 5
                byte[] endFragment = new byte[lastPosition - buffer.position()];
                buffer.get(endFragment);
                buffer.clear();
                buffer.put(endFragment);
            } else {
                buffer.clear();
            }
            HANDLERS.forEach(handler -> handler.onEndOfInterval());
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void saveBytesToBackupFile(byte[] data) {
        backupDataCounter++;
        backupDataBuffer.put(data);
        LOGGER.debug("Added {} bytes to backupDataBuffer", data.length);
        if (backupDataCounter >= 15) {
            try {
                byte[] backupData = new byte[backupDataBuffer.position()];
                backupDataBuffer.position(0);
                backupDataBuffer.get(backupData);
                Files.write(backupFilePath, backupData, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                LOGGER.info("Added {} bytes to the backup file", backupData.length);
            } catch (IOException e) {
                LOGGER.error("saveBytesToBackupFile failed. Exception message: {}", e.getMessage(), e);
            } finally {
                backupDataCounter = 0;
                backupDataBuffer.position(0);
            }
        }
    }

    public static void registerNarcotrackEventListener(NarcotrackEventHandler handler) {
        EEGEvent.getEventHandlers().add(handler);
        CurrentAssessmentEvent.getEventHandlers().add(handler);
        PowerSpectrumEvent.getEventHandlers().add(handler);
        ElectrodeCheckEvent.getEventHandlers().add(handler);
        RemainsEvent.getEventHandlers().add(handler);
        HANDLERS.add(handler);
    }

}
