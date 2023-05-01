package com.mayer;

import com.fazecast.jSerialComm.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class Playground {

    private static final Logger logger = LoggerFactory.getLogger(Playground.class);
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

        logger.info("System Variable TOKEN {}", System.getenv("TOKEN"));
        logger.info("System Variable TOKEN2 {}", System.getenv("TOKEN2"));
        logger.info("System Variable TOKEN NO {}", System.getenv("TOKEN3"));
        logger.info("Test Hi Tim");
    }
}