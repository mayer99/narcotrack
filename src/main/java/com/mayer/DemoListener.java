package com.mayer;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListener;
import com.mayer.handler.*;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;

public class DemoListener implements SerialPortMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(DemoListener.class);
    private final byte  endByte = (byte)0xFE;



    public DemoListener() {
        logger.info("starting...");
    }

    @Override
    public byte[] getMessageDelimiter() {
        return new byte[] {endByte};
    }

    @Override
    public boolean delimiterIndicatesEndOfMessage() {
        return true;
    }

    @Override
    public int getListeningEvents() {
        return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
    }

    @Override
    public void serialEvent(SerialPortEvent serialPortEvent) {
        logger.debug("SerialPortEvent, length of data is {}", serialPortEvent.getReceivedData().length);

    }

}
