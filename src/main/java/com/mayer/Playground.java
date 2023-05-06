package com.mayer;

import com.mayer.frames.NarcotrackFrames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Comparator;


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

        logger.info("Smallest ist {}", Arrays.stream(NarcotrackFrames.values()).min(Comparator.comparing(NarcotrackFrames::getLength)).get());
        logger.info(System.getenv("TESTERERERER"));
    }
}