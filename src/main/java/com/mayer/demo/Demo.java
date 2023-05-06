package com.mayer.demo;

import com.fazecast.jSerialComm.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Demo {

    private static final Logger logger = LoggerFactory.getLogger(Demo.class);
    //private final String serialPortDescriptor = "COM1";
    private final String serialPortDescriptor = "/dev/ttyUSB0";

    private SerialPort serialPort;


    private Demo() {
        logger.info("Application starting...");
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
            logger.error("Shutdown Hook triggered");
            if (serialPort != null && serialPort.isOpen()) {
                serialPort.closePort();
            }
        }
    }

    private void openSerialConnection() {
        logger.debug("Connecting to serial port using descriptor {}", serialPortDescriptor);
        serialPort = SerialPort.getCommPort(serialPortDescriptor);
        serialPort.setBaudRate(115200);
        serialPort.setNumDataBits(8);
        serialPort.setParity(SerialPort.NO_PARITY);
        serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
        if(serialPort.openPort()) {
            logger.info("Connected to serial port");
        } else {
            logger.error("Could not connect to serial port, serialPortDescriptor: {}", serialPortDescriptor);
            logger.error("There are {} comm ports available", SerialPort.getCommPorts().length);
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
