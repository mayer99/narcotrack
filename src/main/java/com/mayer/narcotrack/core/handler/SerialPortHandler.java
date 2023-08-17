package com.mayer.narcotrack.core.handler;

import com.fazecast.jSerialComm.SerialPort;
import com.mayer.narcotrack.core.Narcotrack;
import com.mayer.narcotrack.core.events.*;
import com.mayer.narcotrack.core.models.NarcotrackEventHandler;
import com.mayer.narcotrack.core.models.NarcotrackFrameType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SerialPortHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SerialPortHandler.class);
    private static final byte START_BYTE = (byte)0xFF;
    private static final byte END_BYTE = (byte)0xFE;

    private final ByteBuffer buffer;
    private final long startTimeReference;
    private final BackupFileHandler backupFileHandler;
    private SerialPort serialPort;
    private int intervalsWithoutData = 0;


    public SerialPortHandler(Narcotrack narcotrack) {
        buffer = ByteBuffer.allocate(50000).order(ByteOrder.LITTLE_ENDIAN);
        startTimeReference = System.nanoTime();
        backupFileHandler = new BackupFileHandler(narcotrack);
        try {
            String SERIAL_DESCRIPTOR = System.getenv("NARCOTRACK_SERIAL_DESCRIPTOR");
            if (SERIAL_DESCRIPTOR == null || SERIAL_DESCRIPTOR.trim().isEmpty()) {
                LOGGER.error("Could not find NARCOTRACK_SERIAL_DESCRIPTOR. Maybe, environment variables are not loaded?");
                Narcotrack.rebootPlatform();
                return;
            }
            LOGGER.info("Connecting to serial port using descriptor {}", SERIAL_DESCRIPTOR);
            serialPort = SerialPort.getCommPort(SERIAL_DESCRIPTOR);
            serialPort.setBaudRate(115200);
            serialPort.setNumDataBits(8);
            serialPort.setParity(SerialPort.NO_PARITY);
            serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
            serialPort.openPort();
            Runtime.getRuntime().addShutdownHook(new SerialPortShutdownHook());
            LOGGER.info("Connected to serial port");
            ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
            ScheduledFuture<?> scheduledFuture = ses.scheduleAtFixedRate(this::readSerialPortData, 1, 1, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOGGER.error("Could not connect to serial port", e);
            try {
                LOGGER.warn("Available serial ports are: {}", Arrays.stream(SerialPort.getCommPorts()).map(SerialPort::getSystemPortPath).collect(Collectors.joining(", ")));
            } catch (Exception ex) {
                LOGGER.error("Could not list all available serial ports", ex);
            }
            Narcotrack.rebootPlatform();
        }
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

    private synchronized void readSerialPortData() {
        int bytesAvailable = serialPort.bytesAvailable();
        LOGGER.debug("{} bytes available", bytesAvailable);
        if (bytesAvailable <= 0) {
            intervalsWithoutData++;
            if (intervalsWithoutData%60 == 0) {
                LOGGER.warn("No data received for {}mins", intervalsWithoutData/60);
                if (intervalsWithoutData/60 >= 5) {
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
            LOGGER.error("Could not calculate timeDifference. startTimeReference was {} (ns) and currentTime is {} (ns). currentTime is not 100% accurate", System.nanoTime(), startTimeReference, e);
            time = -1;
        }
        byte[] data = new byte[bytesAvailable];
        serialPort.readBytes(data, data.length);
        backupFileHandler.writeBytes(data);

        if (data.length + buffer.position() > buffer.capacity()) {
            // buffer.position() ist 1 größer als der tatsächliche Füllzustand des Buffers. buffer.capacity() entspricht exakt der Größe.
            // Angenommen cap wäre 10, enthalten sind 5 (dadurch buffer.position() -> 5) und 5 bytesAvailable => 10 -> nicht größer als capacity() (10), daher okay
            // Angenommen cap wäre 10, enthalten sind 5 (dadurch buffer.position() -> 5) und 6 bytesAvailable => 11 > 10 -> abgelehnt
            LOGGER.warn("data would overfill the remaining buffer space. Moving existing bytes in buffer to remains (data.length: {}, buffer.position(): {}, buffer.capacity(): {}).", data.length, buffer.position(), buffer.capacity());
            // Move Buffer to remains!
            new RemainsEvent(time, buffer);
            buffer.clear();
            if (data.length > buffer.capacity()) {
                LOGGER.warn("data would overfill the entire buffer space. Moving data to remains");
                // Move bytesAvailable to remains
                new RemainsEvent(time, data);
                return;
            }
        }
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
                        case EEG -> new EEGEvent(time, buffer);
                        case CURRENT_ASSESSMENT -> new CurrentAssessmentEvent(time, buffer);
                        case POWER_SPECTRUM -> new PowerSpectrumEvent(time, buffer);
                        case ELECTRODE_CHECK -> new ElectrodeCheckEvent(time, buffer);
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
        Narcotrack.getHandlers().forEach(NarcotrackEventHandler::onEndOfInterval);
    }

}
