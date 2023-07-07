package com.mayer.listeners;

import com.mayer.InternalAPIConfig;
import com.mayer.Narcotrack;
import com.mayer.NarcotrackEventHandler;
import com.mayer.events.EEGEvent;
import io.socket.client.IO;
import io.socket.client.Socket;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;

public class EEGMonitorHandler implements NarcotrackEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EEGMonitorHandler.class);

    private Socket socket;
    private final ByteBuffer bufferChannel1;
    private final ByteBuffer bufferChannel2;

    public EEGMonitorHandler() {
        bufferChannel1 = ByteBuffer.allocate(5000).order(ByteOrder.LITTLE_ENDIAN);
        bufferChannel2 = ByteBuffer.allocate(5000).order(ByteOrder.LITTLE_ENDIAN);
        try {
            URI uri = URI.create(InternalAPIConfig.URL);
            IO.Options options = IO.Options.builder()
                    .setAuth(Collections.singletonMap("token", InternalAPIConfig.TOKEN))
                    .build();
            socket = IO.socket(uri, options);
            socket.connect();
            EEGEvent.getEventHandlers().add(this);
            Narcotrack.getEventHandlers().add(this);
        } catch (Exception e) {
            LOGGER.error("Could not initialize EEGMonitorHandler. Exception: {}", e.getMessage(), e);
        }
    }

    public String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits);
    }

    public String encodeHexString(byte[] byteArray) {
        StringBuffer hexStringBuffer = new StringBuffer();
        for (int i = 0; i < byteArray.length; i++) {
            hexStringBuffer.append(byteToHex(byteArray[i]));
        }
        return hexStringBuffer.toString();
    }

    @Override
    public void onEEGEvent(EEGEvent event) {
        if (event.getData().getRaw().length < 36) {
            LOGGER.warn("Raw data of EEGEvent is too short");
            return;
        }
        // ff2800f1304f304f304f304f304f304f304f304f48fd1cfde6fcbafc8ffc6efc4dfc2dfc04590ffe
        bufferChannel1.put(Arrays.copyOfRange(event.getData().getRaw(), 4, 20));
        bufferChannel2.put(Arrays.copyOfRange(event.getData().getRaw(), 20, 36));
    }

    @Override
    public void onEndOfInterval() {
        int endPosition = bufferChannel1.position();
        if (!(endPosition%2 == 0 && endPosition%4 == 0 && endPosition%8 == 0)) {
            LOGGER.warn("bytes in bufferChannel1 are not dividable by 2, 4 or 8");
            bufferChannel1.clear();
            return;
        }
        bufferChannel1.position(0);
        byte[] eegValues1Byte = new byte[endPosition];
        bufferChannel1.get(eegValues1Byte);

        bufferChannel1.position(0);
        short[] eegValues2Bytes = new short[endPosition/2];
        for (int i = 0; i < eegValues2Bytes.length; i++) {
            eegValues2Bytes[i] = bufferChannel1.getShort();
        }

        bufferChannel1.position(0);
        int[] eegValues4Bytes = new int[endPosition/4];
        for (int i = 0; i < eegValues4Bytes.length; i++) {
            eegValues4Bytes[i] = bufferChannel1.getInt();
        }

        bufferChannel1.position(0);
        long[] eegValues8Bytes = new long[endPosition/8];
        for (int i = 0; i < eegValues8Bytes.length; i++) {
            eegValues8Bytes[i] = bufferChannel1.getLong();
        }

        bufferChannel1.clear();
        bufferChannel2.clear();

        JSONObject data = new JSONObject();
        try {
            data.put("channel1Byte", eegValues1Byte);
            data.put("channel1Short", eegValues2Bytes);
            data.put("channel1Int", eegValues4Bytes);
            data.put("channel1Long", eegValues8Bytes);
            socket.emit("eeg", data);
        } catch (Exception e) {
            LOGGER.warn("Could not send eeg data, Exception message: {}", e.getMessage(), e);
        }
    }
}
