package com.mayer.playground;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class Playground {

    static int i = 0;

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


        long start = System.nanoTime();
        ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
        ScheduledFuture<?> scheduledFuture = ses.scheduleAtFixedRate(() -> {

            try {
                Connection databaseConnection = DriverManager.getConnection("jdbc:mariadb://mayer.local");
            } catch (SQLException e) {
                LOGGER.error("PRoblem, Exception Message: {}", e.getMessage(), e);
            }

            //LOGGER.info("Scheduled Future called. Time is {}", TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS));

        }, 2, 2, TimeUnit.SECONDS);

    }
}