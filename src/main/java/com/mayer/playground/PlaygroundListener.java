package com.mayer.playground;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlaygroundListener implements SerialPortMessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlaygroundListener.class);

    private final byte START_BYTE = (byte)0xFF;
    private final byte END_BYTE = (byte)0xFE;


    public PlaygroundListener() {
        LOGGER.info("starting...");
    }

    @Override
    public byte[] getMessageDelimiter() {
        return new byte[] {END_BYTE};
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
    public synchronized void serialEvent(SerialPortEvent serialPortEvent) {
        LOGGER.debug("SerialPortEvent (length: {})", serialPortEvent.getReceivedData().length);
    }
}
