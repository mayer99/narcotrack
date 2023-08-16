package com.mayer.logging;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;

import java.util.ArrayList;

public class Throwable {

    private final String message;
    private final ArrayList<String> trace;
    public Throwable(IThrowableProxy throwable) {
        message = throwable.getMessage();
        trace = new ArrayList<>();
        if (throwable.getStackTraceElementProxyArray() != null) {
            StackTraceElementProxy[] stackTraceElementProxies = throwable.getStackTraceElementProxyArray();
            for (int i = 0; i < 5 && i < stackTraceElementProxies.length; i++) {
                if (stackTraceElementProxies[i].getStackTraceElement() != null) {
                    trace.add(stackTraceElementProxies[i].getStackTraceElement().toString());
                }
            }
        }
    }
}
