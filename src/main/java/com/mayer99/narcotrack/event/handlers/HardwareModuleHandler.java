package com.mayer99.narcotrack.event.handlers;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;
import com.mayer99.narcotrack.event.NarcotrackEventHandler;
import com.mayer99.narcotrack.event.NarcotrackEventManager;
import com.mayer99.narcotrack.application.NarcotrackApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Collectors;

public class HardwareModuleHandler implements NarcotrackEventHandler {

    private final Logger LOGGER = LoggerFactory.getLogger(HardwareModuleHandler.class);

    private final NarcotrackApplication application;
    private final NarcotrackEventManager eventManager;
    private SerialPort serialPort;

    public HardwareModuleHandler(NarcotrackApplication application) {
        LOGGER.info("Starting HardwareModuleHandler...");
        this.application = application;
        eventManager = application.getEventManager();
        initSerialPort();
        Runtime.getRuntime().addShutdownHook(new HardwareHandlerShutdownHook());
    }

    private void initSerialPort() {
        String serialPortDescriptor = application.getConfig("HARDWARE_MODULE_PORT");
        if (serialPortDescriptor == null || serialPortDescriptor.trim().isEmpty()) {
            LOGGER.error("HARDWARE_PORT is null, empty or whitespace. Please check the configuration file");
            eventManager.dispatchOnUnrecoverableError();
            System.exit(1);
        }
        LOGGER.info("Connecting to SerialPort using descriptor {}...", serialPortDescriptor);
        try {
            serialPort = SerialPort.getCommPort(serialPortDescriptor);
        } catch (SerialPortInvalidPortException e) {
            LOGGER.error("Could not find SerialPort with descriptor {}", serialPortDescriptor);
            try {
                LOGGER.info("Available SerialPortDescriptors are {}", Arrays.stream(SerialPort.getCommPorts()).map(SerialPort::getSystemPortPath).collect(Collectors.joining(", ")));
            } catch (Exception ex) {
                LOGGER.error("Could not list all SerialPortDescriptors", ex);
            }
            eventManager.dispatchOnRecoverableError();
            application.scheduleRestart();
            System.exit(1);

        }
        serialPort.setComPortParameters(9600, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        if (!serialPort.openPort()) {
            LOGGER.error("Could not open SerialPort");
            eventManager.dispatchOnRecoverableError();
            application.scheduleRestart();
            System.exit(1);
        }
        LOGGER.info("Connected to SerialPort for HardwareModuleHandler");
    }

    class HardwareHandlerShutdownHook extends Thread {
        @Override
        public void run() {
            LOGGER.warn("HardwareHandlerShutdownHook triggered");
            if (serialPort != null && serialPort.isOpen()) {
                LOGGER.info("Attempting to close SerialPort");
                if (serialPort.closePort()) {
                    LOGGER.info("Closed SerialPort");
                } else {
                    LOGGER.error("Could not close SerialPort");
                }
            }
        }
    }

    @Override
    public void onSystemStart() {
        LOGGER.info("Changing StatusLights to IdleAnimation");
        sendCommand("statuslights:animate?animation=fadein&red=0&green=0&blue=255&brightness=0.3");
    }

    @Override
    public void onIntervalEnd() {
        sendCommand("system:keepalive");
    }

    @Override
    public void onRecordingStart(Instant time) {
        sendCommand("statuslights:animate?animation=fadeout&red=0&green=0&blue=255&brightness=0.3&now");
        sendCommand("statuslights:animate?animation=loading&red=0&green=255&blue=0&brightness=0.5&duration=2000&infinite");
    }

    @Override
    public void onRecordingStop() {
        sendCommand("statuslights:animate?animation=fadein&red=0&green=0&blue=255&brightness=0.3");
    }

    @Override
    public void onGoodElectrodes() {
        sendCommand("statuslights:colorchange?red=0&green=255&blue=0");
    }

    @Override
    public void onLooseElectrode() {
        sendCommand("statuslights:colorchange?red=255&green=80&blue=0");
    }

    @Override
    public void onDetachedElectrode() {
        sendCommand("statuslights:colorchange?red=255&green=0&blue=0");
    }

    @Override
    public void onRecoverableError() {
        sendCommand("system:restart");
    }

    @Override
    public void onUnrecoverableError() {
        sendCommand("system:error");
    }

    private void sendCommand(String command) {
        LOGGER.debug("Sending command {} to HardwareModule", command);
        command += "\n";
        byte[] bytes = command.getBytes();
        serialPort.writeBytes(bytes, bytes.length);
    }
}
