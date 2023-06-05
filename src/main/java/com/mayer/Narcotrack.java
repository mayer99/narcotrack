package com.mayer;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Narcotrack {

    private static final Logger LOGGER = LoggerFactory.getLogger(Narcotrack.class);

    private final String narcotrackSerialDescriptor = System.getenv("NARCOTRACK_SERIAL_DESCRIPTOR");

    private SerialPort serialPort;

    private Narcotrack() {
        LOGGER.info("Application starting...");
        openSerialConnection();
        Runtime.getRuntime().addShutdownHook(new SerialPortShutdownHook());
        serialPort.addDataListener(new SerialPortDisconnectListener());
        serialPort.addDataListener(new NarcotrackListener());
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

    private void openSerialConnection() {
        if (narcotrackSerialDescriptor == null || narcotrackSerialDescriptor.trim().isEmpty()) {
            LOGGER.error("Could not find serialPortDescriptor. Maybe, environment variables are not loaded?");
            rebootPlatform();
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

}
