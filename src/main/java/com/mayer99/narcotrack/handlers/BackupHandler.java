package com.mayer99.narcotrack.handlers;

import com.mayer99.narcotrack.base.models.NarcotrackEventHandler;
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

public class BackupHandler implements NarcotrackEventHandler {

    private static final int MAX_BACKUP_BATCHES = 30;

    private static final Logger logger = LoggerFactory.getLogger(BackupHandler.class);

    private final SimpleDateFormat sdf;
    private final ByteBuffer backupBuffer;

    private String backupFileName;
    private Path backupFilePath;
    private int backupCounter = 0;

    public BackupHandler() {
        logger.info("BackupHandler starting...");
        sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        backupBuffer = ByteBuffer.allocate(200_000).order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public void onRecordingStart(Instant instant) {
        backupFileName = sdf.format(Date.from(instant)) + ".bin";
        backupFilePath = Paths.get("backups", backupFileName);
        logger.info("BackupFile is called {} and can be found in {}", backupFileName, backupFilePath);
    }

    @Override
    public void onRecordingStop() {
        logger.debug("Entering2");
        offloadBackup();
        backupFileName = null;
        backupFilePath = null;
        backupCounter = 0;
        logger.debug("Backup Offloaded and stopped");
    }

    @Override
    public void onBackup(byte[] data) {
        if (data.length + backupBuffer.position() > backupBuffer.capacity()) {
            logger.warn("BackupBuffer contains {}/{} bytes after {} calls", backupBuffer.position(), backupBuffer.capacity(), backupCounter);
            backupCounter = 0;
            logger.warn("New data of length {} would overflow remaining buffer space, moving contents to file", data.length);
            offloadBackup();

            if (data.length > backupBuffer.capacity()) {
                logger.error("New data of length {} would overflow buffer, offloading new data immediately", data.length);
                saveToBackupFile(data);
                return;
            }
        }
        backupCounter++;
        backupBuffer.put(data);
        logger.debug("Added {} bytes to BackupBuffer. BackupBuffer now contains {}/{} bytes after {} calls", data.length, backupBuffer.position(), backupBuffer.capacity(), backupCounter);
        if (backupCounter >= MAX_BACKUP_BATCHES) {
            backupCounter = 0;
            offloadBackup();
        }
    }
    private void offloadBackup() {
        byte[] data = new byte[backupBuffer.position()];
        backupBuffer.position(0);
        backupBuffer.get(data);
        backupBuffer.clear();
        saveToBackupFile(data);
    }

    private void saveToBackupFile(byte[] data) {
        try {
            Files.write(backupFilePath, data, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            logger.info("Added {} bytes to the backup file", data.length);
        } catch (IOException e) {
            logger.error("Could not offload backup", e);
        }
    }


}
