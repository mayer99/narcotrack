package com.mayer;

import com.fazecast.jSerialComm.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Narcotrack {

    private static final Logger logger = LoggerFactory.getLogger(Narcotrack.class);
    private static final String NARCOTRACK_SERIAL_DESCRIPTOR = System.getenv("NARCOTRACK_SERIAL_DESCRIPTOR");
    private static final String NARCOTRACK_DB_URL = System.getenv("NARCOTRACK_DB_URL");
    private static final String NARCOTRACK_DB_TABLE = System.getenv("NARCOTRACK_DB_TABLE");
    private static final String NARCOTRACK_DB_USERNAME = System.getenv("NARCOTRACK_DB_USERNAME");
    private static final String NARCOTRACK_DB_PASSWORD = System.getenv("NARCOTRACK_DB_PASSWORD");

    private SerialPort serialPort;
    private Connection databaseConnection;


    private Narcotrack() {
        logger.info("Application starting...");
        openSerialConnection();
        openDatabaseConnection();
        Runtime.getRuntime().addShutdownHook(new NarcotrackShutdownHook(serialPort, databaseConnection));
        serialPort.addDataListener(new NarcotrackListener(databaseConnection));
    }

    private void openSerialConnection() {

        if (NARCOTRACK_SERIAL_DESCRIPTOR == null || NARCOTRACK_SERIAL_DESCRIPTOR.isEmpty()) {
            logger.error("Could not find serialPortDescriptor. Maybe, environment variables are not loaded?");
        }
        try {
            logger.debug("Connecting to serial port using descriptor {}", NARCOTRACK_SERIAL_DESCRIPTOR);
            serialPort = SerialPort.getCommPort(NARCOTRACK_SERIAL_DESCRIPTOR);
            serialPort.setBaudRate(115200);
            serialPort.setNumDataBits(8);
            serialPort.setParity(SerialPort.NO_PARITY);
            serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
            serialPort.openPort();
            logger.info("Connected to serial port");
        } catch (Exception e) {
            logger.error("Could not connect to serial port, serialPortDescriptor: {}", NARCOTRACK_SERIAL_DESCRIPTOR, e);
            try {
                if (!Arrays.stream(SerialPort.getCommPorts()).anyMatch(serialPort -> serialPort.getSystemPortPath().equalsIgnoreCase(NARCOTRACK_SERIAL_DESCRIPTOR))) {
                    logger.error("Port Descriptor {} does not match any of the available serial ports. Available ports are {}", NARCOTRACK_SERIAL_DESCRIPTOR, Arrays.stream(SerialPort.getCommPorts()).map(sp -> sp.getSystemPortPath()).collect(Collectors.joining(" ")));
                } else {
                    logger.error("Port Descriptor matches one of the available serial ports, but connection could not be opened");
                }
            } catch (Exception ex) {

            }
            System.exit(1);
        }
    }

    private void openDatabaseConnection() {
        logger.debug("Connecting to database");
        try {
            databaseConnection = DriverManager.getConnection("jdbc:mariadb://" + NARCOTRACK_DB_URL + ":3306/" + NARCOTRACK_DB_TABLE, NARCOTRACK_DB_USERNAME, NARCOTRACK_DB_PASSWORD);
            logger.info("Connected to database");
        } catch (SQLException e) {
            logger.error("Could not connect to database", e);
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        new Narcotrack();
    }

}
