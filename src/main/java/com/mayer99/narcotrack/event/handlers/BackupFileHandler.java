package com.mayer99.narcotrack.event.handlers;


import com.mayer99.narcotrack.event.NarcotrackEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

public class BackupFileHandler implements NarcotrackEventHandler {

    private final int MAX_BACKUP_BATCHES = 30;
    private final Logger LOGGER = LoggerFactory.getLogger(BackupFileHandler.class);
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    private final ByteBuffer buffer = ByteBuffer.allocate(200_000).order(ByteOrder.LITTLE_ENDIAN);
    private String backupFileName;
    private Path backupFilePath;
    private int currentBatchCount = 0;

    public BackupFileHandler() {
        LOGGER.info("BackupFileHandler starting...");
    }

    @Override
    public void onRecordingStart(Instant instant) {
        backupFileName = sdf.format(Date.from(instant)) + ".bin";
        backupFilePath = Paths.get("backups", backupFileName);
        LOGGER.info("BackupFile is called {} and can be found here: {}", backupFileName, backupFilePath);
    }

    @Override
    public void onRecordingStop() {
        saveBufferToBackupFile();
        buffer.clear();
        backupFileName = null;
        backupFilePath = null;
        currentBatchCount = 0;
        LOGGER.debug("Finished backup");
    }

    @Override
    public void onReceivedData(byte[] data) {
        LOGGER.debug("BackupBuffer contains {}/{} bytes after {} calls", buffer.position(), buffer.capacity(), currentBatchCount);
        if (data.length + buffer.position() > buffer.capacity()) {
            LOGGER.warn("New data would overflow remaining buffer space");
            saveBufferToBackupFile();
            if (data.length > buffer.capacity()) {
                LOGGER.warn("New data would overflow the buffer, moving new data to remains");
                saveToBackupFile(data);
                return;
            }
        }
        currentBatchCount++;
        buffer.put(data);
        LOGGER.debug("Added {} bytes to BackupBuffer. BackupBuffer now contains {}/{} bytes after {} calls", data.length, buffer.position(), buffer.capacity(), currentBatchCount);
        if (currentBatchCount >= MAX_BACKUP_BATCHES) {
            LOGGER.debug("Reached maximum currentBatchCount, saving buffer to backup file");
            currentBatchCount = 0;
            saveBufferToBackupFile();
        }
    }

    private void saveBufferToBackupFile() {
        try (FileChannel channel = FileChannel.open(backupFilePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            buffer.flip();
            int bytesWritten = channel.write(buffer);
            buffer.clear();
            LOGGER.info("Added {} bytes of the buffer to the backup file", bytesWritten);
        } catch (IOException e) {
            LOGGER.error("Could not save to backupFile", e);
        }
    }

    private void saveToBackupFile(byte[] data) {
        try {
            Files.write(backupFilePath, data, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            LOGGER.info("Added {} bytes to the backup file", data.length);
        } catch (IllegalArgumentException | IOException | UnsupportedOperationException | SecurityException e) {
            LOGGER.error("Could not save to backupFile", e);
        }
    }

}
