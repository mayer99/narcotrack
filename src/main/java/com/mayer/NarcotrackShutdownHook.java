package com.mayer;

import com.fazecast.jSerialComm.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class NarcotrackShutdownHook extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(NarcotrackShutdownHook.class);
    private SerialPort serialPort;
    private Connection databaseConnection;
    private static final String NARCOTRACK_DEV_MODE = System.getenv("NARCOTRACK_DEV_MODE");

    public NarcotrackShutdownHook(SerialPort serialPort, Connection databaseConnection) {
        this.serialPort = serialPort;
        this.databaseConnection = databaseConnection;
    }

    public void run() {
        logger.error("Shutdown Hook triggered");
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
        }
        try {
            if (databaseConnection != null && !databaseConnection.isClosed()) {
                databaseConnection.close();
            }
        } catch (SQLException e) {
            logger.error("Error closing database connection", e);
        }
    }
}
