package com.mayer.demo;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
