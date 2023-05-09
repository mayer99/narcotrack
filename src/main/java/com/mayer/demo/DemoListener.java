package com.mayer.demo;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DemoListener implements SerialPortMessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoListener.class);
    private static final byte END_BYTE = (byte)0xFE;



    public DemoListener() {
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
    public void serialEvent(SerialPortEvent serialPortEvent) {
        LOGGER.debug("SerialPortEvent, length of data is {}", serialPortEvent.getReceivedData().length);

    }

}
