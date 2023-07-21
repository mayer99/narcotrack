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
    private ByteBuffer bufferChannel1;
    private ByteBuffer bufferChannel2;

    public EEGMonitorHandler() {
        if (System.getenv("NARCOTRACK_EEG_MONITOR") != "ON") {
            LOGGER.info("EEG monitor connection turned off");
            return;
        }
        bufferChannel1 = ByteBuffer.allocate(5000).order(ByteOrder.LITTLE_ENDIAN);
        bufferChannel2 = ByteBuffer.allocate(5000).order(ByteOrder.LITTLE_ENDIAN);
        try {
            URI uri = URI.create(InternalAPIConfig.URL);
            IO.Options options = IO.Options.builder()
                    .setAuth(Collections.singletonMap("token", InternalAPIConfig.TOKEN))
                    .build();
            socket = IO.socket(uri, options);
            socket.connect();
            Narcotrack.registerNarcotrackEventListener(this);
        } catch (Exception e) {
            LOGGER.error("Could not initialize EEGMonitorHandler. Exception: {}", e.getMessage(), e);
        }
    }


    @Override
    public void onEEGEvent(EEGEvent event) {
        if (event.getData().getRaw().length < 36) {
            LOGGER.warn("Raw data of EEGEvent is too short");
            return;
        }
        bufferChannel1.put(Arrays.copyOfRange(event.getData().getRaw(), 4, 20));
        bufferChannel2.put(Arrays.copyOfRange(event.getData().getRaw(), 20, 36));
    }

    public int[] getIntValues(ByteBuffer buffer) {
        int[] values = new int[buffer.position()/2];
        buffer.position(0);
        for (int i = 0; i < values.length; i++) {
            values[i] = Short.toUnsignedInt(buffer.getShort());
        }
        return values;
    }

    public short[] getShortValues(ByteBuffer buffer) {
        short[] values = new short[buffer.position()/2];
        buffer.position(0);
        for (int i = 0; i < values.length; i++) {
            values[i] = buffer.getShort();
        }
        return values;
    }

    @Override
    public void onEndOfInterval() {

        if (bufferChannel1.position()%2 != 0) {
            LOGGER.warn("bytes in bufferChannel1 are not dividable by 2");
            bufferChannel1.clear();
            return;
        }
        if (bufferChannel2.position()%2 != 0) {
            LOGGER.warn("bytes in bufferChannel1 are not dividable by 2");
            bufferChannel2.clear();
            return;
        }

        JSONObject data = new JSONObject();
        try {
            data.put("channel1", getShortValues(bufferChannel1));
            data.put("channel1U", getIntValues(bufferChannel1));
            data.put("channel2", getShortValues(bufferChannel2));
            data.put("channel2U", getIntValues(bufferChannel2));
            socket.emit("eeg", data);
        } catch (Exception e) {
            LOGGER.warn("Could not send eeg data, Exception message: {}", e.getMessage(), e);
        } finally {
            bufferChannel1.clear();
            bufferChannel2.clear();
        }
    }

}
