package com.mayer;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Narcotrack {

    private static final Logger LOGGER = LoggerFactory.getLogger(Narcotrack.class);

    private final String narcotrackSerialDescriptor = System.getenv("NARCOTRACK_SERIAL_DESCRIPTOR");

    private SerialPort serialPort;

    private Narcotrack() {
        LOGGER.info("Application starting...");
        openSerialConnection();
        openDatabaseConnection();
        Runtime.getRuntime().addShutdownHook(new SerialPortShutdownHook());
        serialPort.addDataListener(new NarcotrackListener());
        serialPort.addDataListener(new SerialPortDisconnectListener());
    }

    class SerialPortShutdownHook extends Thread {

        @Override
        public void run() {
            LOGGER.error("Shutdown Hook triggered");
            if (serialPort != null && serialPort.isOpen()) {
                serialPort.closePort();
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
        if (narcotrackSerialDescriptor == null || narcotrackSerialDescriptor.isEmpty()) {
            LOGGER.error("Could not find serialPortDescriptor. Maybe, environment variables are not loaded?");
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
            LOGGER.error("Could not connect to serial port, serialPortDescriptor: {}", narcotrackSerialDescriptor, e);
            try {
                if (!Arrays.stream(SerialPort.getCommPorts()).anyMatch(serialPort -> serialPort.getSystemPortPath().equalsIgnoreCase(narcotrackSerialDescriptor))) {
                    LOGGER.error("Port Descriptor {} does not match any of the available serial ports. Available ports are {}", narcotrackSerialDescriptor, Arrays.stream(SerialPort.getCommPorts()).map(sp -> sp.getSystemPortPath()).collect(Collectors.joining(" ")));
                } else {
                    LOGGER.error("Port Descriptor matches one of the available serial ports, but connection could not be opened");
                }
            } catch (Exception ex) {

            }
            System.exit(1);
        }
    }

    private void openDatabaseConnection() {

    }

    public static void main(String[] args) {
        new Narcotrack();
    }

}
