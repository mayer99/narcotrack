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
        initializeSerialPort();
    }

    private void logSerialPortDescriptors() {
        try {
            LOGGER.info("SerialPortDescriptors are {}", Arrays.stream(SerialPort.getCommPorts()).map(SerialPort::getSystemPortPath).collect(Collectors.joining(", ")));
        } catch (Exception e) {
            LOGGER.error("Could not list SerialPortDescriptors", e);
        }
    }

    private void initializeSerialPort() {
        try {
            String serialPortDescriptor = NarcotrackApplication.getEnvironmentVariable("HARDWARE_MODULE_PORT");
            serialPort = SerialPort.getCommPort(serialPortDescriptor);
            serialPort.setComPortParameters(9600, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
            if (!serialPort.openPort()) {
                throw new RuntimeException("Failed to open SerialPort.");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to initialize SerialPort.", e);
            logSerialPortDescriptors();
            eventManager.dispatchOnRecoverableError();
            application.scheduleRestart();
            application.cleanupAndExit();
        }
    }

    @Override
    public void cleanup() {
        LOGGER.warn("cleanup...");
        if (serialPort != null && serialPort.isOpen()) {
            LOGGER.info("Attempting to close SerialPort");
            if (serialPort.closePort()) {
                LOGGER.info("Closed SerialPort");
            } else {
                LOGGER.error("Could not close SerialPort");
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
        if (serialPort.writeBytes(bytes, bytes.length) < 0) {
            LOGGER.error("Could not write bytes to SerialPort");
            eventManager.dispatchOnRecoverableError();
            application.scheduleRestart();
            application.cleanupAndExit();
        }
    }
}
