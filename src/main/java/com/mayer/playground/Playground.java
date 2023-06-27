package com.mayer.playground;

import com.fazecast.jSerialComm.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class Playground {
    private static final Logger LOGGER = LoggerFactory.getLogger(Playground.class);
    public static void main(String[] args) {
        // 00 2a -> 42
        //System.out.println((short) (((byte) 0x2a) & 0xff));
        //System.out.println(String.format("%02x", (byte)0xFE));
        //NarcotrackMonitoring monitoring = new NarcotrackMonitoring();
        //monitoring.send("Hi");
        //logger.info("Example log from {}", Playground.class.getSimpleName());
        //new NarcotrackStatistics();
        //ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
        //ScheduledFuture<?> scheduledFuture = ses.scheduleAtFixedRate(() -> {
        //    logger.info("Test Log");
        //}, 3, 5, TimeUnit.SECONDS);
        //DecimalFormat decimalFormat = new DecimalFormat("0.00");
        //logger.info(decimalFormat.format((double) 1 /5));

        //logger.info("1: {}, 2: {}", (new Playground1Event().getTest()), (new Playground2Event().getTest()));
        //logger.info(System.getenv("TESTERERERER"));
        ByteBuffer buffer = ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN);
        byte[] data = new byte[10];
        new Random().nextBytes(data);
        data[3] = 99;
        buffer.put(data);
        LOGGER.info("buffer.position(): {}, buffer.get(3): {}", buffer.position(), buffer.get(3));
        buffer.position(3);
        LOGGER.info("buffer.get(): {}", buffer.get());
    }
}