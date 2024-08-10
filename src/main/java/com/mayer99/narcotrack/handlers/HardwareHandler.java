package com.mayer99.narcotrack.handlers;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;
import com.mayer99.narcotrack.NarcotrackEventHandler;
import com.mayer99.narcotrack.NarcotrackEventManager;
import com.mayer99.narcotrack.application.NarcotrackApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Collectors;

public class HardwareHandler implements NarcotrackEventHandler {

    private final Logger LOGGER = LoggerFactory.getLogger(HardwareHandler.class);

    private final NarcotrackApplication application;
    private final NarcotrackEventManager eventManager;
    private SerialPort serialPort;

    public HardwareHandler(NarcotrackApplication application) {
        LOGGER.info("Starting HardwareHandler...");
        this.application = application;
        eventManager = application.getEventManager();
        initSerialPort();
        Runtime.getRuntime().addShutdownHook(new HardwareHandlerShutdownHook());
    }

    private void initSerialPort() {
        String serialPortDescriptor = application.getConfig("HARDWARE_PORT");
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
                eventManager.dispatchOnRecoverableError();
                application.scheduleRestart();
                System.exit(1);
            } catch (Exception ex) {
                LOGGER.error("Could not list all SerialPortDescriptors", ex);
                eventManager.dispatchOnRecoverableError();
                application.scheduleRestart();
                System.exit(1);
            }

        }
        serialPort.setComPortParameters(9600, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        if (!serialPort.openPort()) {
            LOGGER.error("Could not open SerialPort");
            eventManager.dispatchOnRecoverableError();
            application.scheduleRestart();
            System.exit(1);
        }
        LOGGER.info("Connected to SerialPort for HardwareHandler");
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
        LOGGER.info("Changing StatusLights from StartupAnimation to IdleAnimation");
        sendCommand("statuslights:animate?aniamtion=fadein&red=0&green=0&blue=255&brightness=0.2&duration=1000&transition=endofcycle");
        LOGGER.info("Activating keepalive on HardwareModule");
        sendCommand("system:configure?keepalive=true");
    }

    @Override
    public void onIntervalStop() {
        sendCommand("system:keepalive");
    }

    @Override
    public void onRecordingStart(Instant time) {
        // Altes Licht muss noch ausfaden
        sendCommand("statuslights:animate?aniamtion=loading&red=0&green=255&blue=0&brightness=0.3&duration=1500&infinite");
    }

    @Override
    public void onRecordingStop() {
        sendCommand("statuslights:animate?aniamtion=fadein&red=0&green=0&blue=255&brightness=0.2&duration=1000&transition=endofcycle");
    }

    @Override
    public void onGoodElectrodes() {
        sendCommand("statuslights:colorchange?red=0&green=255&blue=0&duration=1000&transition=smooth");
    }

    @Override
    public void onLooseElectrode() {
        sendCommand("statuslights:colorchange?red=255&green=165&blue=0&duration=1000&transition=smooth");
    }

    @Override
    public void onDetachedElectrode() {
        sendCommand("statuslights:colorchange?red=255&green=0&blue=0&duration=1000&transition=smooth");
    }

    @Override
    public void onRecoverableError() {
        sendCommand("statuslights:animate?aniamtion=error&red=255&green=165&blue=0&brightness=0.5&duration=1500&transition=endofcycle&repeats=10");
        sendCommand("statuslights:animate?aniamtion=startup&red=0&green=0&blue=255&brightness=0.5&duration=1500&transition=endofcycle&infinite");
        sendCommand("system:configure?phase=startup");
    }

    @Override
    public void onUnrecoverableError() {
        sendCommand("statuslights:animate?aniamtion=error&red=255&green=0&blue=0&brightness=0.5&duration=1500&transition=endofcycle&infinite");
        sendCommand("system:configure?keepalive=false");
    }

    private void sendCommand(String command) {
        LOGGER.debug("Sending command {} to HardwareModule", command);
        command += "\n";
        byte[] bytes = command.getBytes();
        serialPort.writeBytes(bytes, bytes.length);
    }
}
