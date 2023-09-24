package com.mayer.old;

import com.mayer.rework.Narcotrack;
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
import java.util.Date;

public class BackupFileHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupFileHandler.class);
    private static final int MAX_BACKUP_DATA_BATCHES = 30;

    private final Path backupFilePath;
    private int backupDataCounter = 0;
    private final ByteBuffer backupDataBuffer;

    public BackupFileHandler(Narcotrack narcotrack) {
        LOGGER.info("");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String backupFileName = sdf.format(Date.from(narcotrack.getStartTime())) + ".bin";
        backupFilePath = Paths.get("backups", backupFileName);
        backupDataBuffer = ByteBuffer.allocate(100_000).order(ByteOrder.LITTLE_ENDIAN);
        LOGGER.info("BackupFileHandler startet. backupFileName: {}", backupFileName);
    }

    public void save(byte[] data) {
        backupDataCounter++;
        if (backupDataBuffer.position() + data.length > backupDataBuffer.capacity()) {
            LOGGER.error("Cannot write data to the backupDataBuffer, buffer would overflow (position: {}, length of new data: {})", backupDataBuffer.position(), data.length);
            return;
        }
        backupDataBuffer.put(data);
        LOGGER.debug("Added {} bytes to backupDataBuffer", data.length);
        if (backupDataCounter >= MAX_BACKUP_DATA_BATCHES) {
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
