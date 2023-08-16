package com.mayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

public class BackupFileHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupFileHandler.class);
    private static final int maxBackupDataBatches = 30;

    private final Path backupFilePath;
    private int backupDataCounter;
    private final ByteBuffer backupDataBuffer;

    public BackupFileHandler(Narcotrack narcotrack) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        Instant startTime = narcotrack.getStartTime();
        String backupFileName = sdf.format(Date.from(startTime)) + ".bin";
        backupFilePath = Paths.get("backups", backupFileName);
        backupDataCounter = 0;
        backupDataBuffer = ByteBuffer.allocate(150_000).order(ByteOrder.LITTLE_ENDIAN);
        LOGGER.info("BackupFileHandler startet. backupFileName: {}", backupFileName);
    }

    public void writeBytes(byte[] data) {
        backupDataCounter++;
        if (backupDataBuffer.position() + data.length > backupDataBuffer.capacity()) {
            LOGGER.error("Cannot write data to the backupDataBuffer, buffer would overflow (position: {}, length of new data: {})", backupDataBuffer.position(), data.length);
            return;
        }
        backupDataBuffer.put(data);
        LOGGER.debug("Added {} bytes to backupDataBuffer", data.length);
        if (backupDataCounter >= maxBackupDataBatches) {
            try {
                byte[] backupData = new byte[backupDataBuffer.position()];
                backupDataBuffer.position(0);
                backupDataBuffer.get(backupData);
                Files.write(backupFilePath, backupData, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                LOGGER.info("Added {} bytes to the backup file", backupData.length);
            } catch (IOException e) {
                LOGGER.error("writeBytes failed", e);
            } finally {
                backupDataCounter = 0;
                backupDataBuffer.position(0);
            }
        }
    }
}
