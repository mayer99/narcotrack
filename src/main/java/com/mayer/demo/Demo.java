package com.mayer.demo;

import com.fazecast.jSerialComm.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Demo {

    private static final Logger LOGGER = LoggerFactory.getLogger(Demo.class);
    //private static final String SERIAL_PORT_DESCRIPTOR = "COM1";
    private static final String SERIAL_PORT_DESCRIPTOR = "/dev/ttyUSB0";

    private SerialPort serialPort;


    private Demo() {
        LOGGER.info("Application starting...");
        openSerialConnection();
        Runtime.getRuntime().addShutdownHook(new SerialPortShutdownHook(serialPort));
        serialPort.addDataListener(new DemoListener());

    }

    class SerialPortShutdownHook extends Thread {

        private final SerialPort serialPort;
        public SerialPortShutdownHook(SerialPort serialPort) {
            this.serialPort = serialPort;
        }

        @Override
        public void run() {
            LOGGER.error("Shutdown Hook triggered");
            if (serialPort != null && serialPort.isOpen()) {
                serialPort.closePort();
            }
        }
    }

    private void openSerialConnection() {
        LOGGER.debug("Connecting to serial port using descriptor {}", SERIAL_PORT_DESCRIPTOR);
        serialPort = SerialPort.getCommPort(SERIAL_PORT_DESCRIPTOR);
        serialPort.setBaudRate(115200);
        serialPort.setNumDataBits(8);
        serialPort.setParity(SerialPort.NO_PARITY);
        serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
        if(serialPort.openPort()) {
            LOGGER.info("Connected to serial port");
        } else {
            LOGGER.error("Could not connect to serial port, serialPortDescriptor: {}", SERIAL_PORT_DESCRIPTOR);
            LOGGER.error("There are {} comm ports available", SerialPort.getCommPorts().length);
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        new Demo();
    }

    public static void log(String message) {
        System.out.println("Narcotrack - " + message);
    }

}
