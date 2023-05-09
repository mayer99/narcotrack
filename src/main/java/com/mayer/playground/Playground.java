package com.mayer.playground;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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



        ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
        ScheduledFuture<?> scheduledFuture = ses.scheduleAtFixedRate(() -> {

            LOGGER.info("Scheduled Future called");

            i++;
            if (i >= 5) {
                if (System.getenv("PLEASE_DO_NOT_RESTART") == null) {
                    try {
                        LOGGER.info("Trying shutdown");
                        Runtime.getRuntime().exec("echo Hi2");
                        Runtime.getRuntime().exec("sudo shutdown -r now");
                    } catch (IOException e) {
                        LOGGER.error("Cannot restart system", e);
                        System.exit(1);
                    }
                }
            }
        }, 0, 2, TimeUnit.SECONDS);

    }
}