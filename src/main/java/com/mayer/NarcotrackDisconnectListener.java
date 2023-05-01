package com.mayer;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NarcotrackDisconnectListener implements SerialPortDataListener {

    private static final Logger logger = LoggerFactory.getLogger(NarcotrackDisconnectListener.class);


    @Override
    public int getListeningEvents() {
        return SerialPort.LISTENING_EVENT_PORT_DISCONNECTED;
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        logger.error("Listener noticed disconnect of serial port");
    }
}
