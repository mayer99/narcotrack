package com.mayer99.playground;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Playground {

    private static final Logger LOGGER = LoggerFactory.getLogger(Playground.class);

    public static void main(String[] args) {
        ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
        ses.scheduleAtFixedRate(() -> {
            LOGGER.info("Testnachricht ohne wirkliche Relevanz");
        }, 1, 1, TimeUnit.SECONDS);
    }

}
