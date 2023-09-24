package com.mayer99.logging;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Objects;

public class Log {

    private final String thread, level, message, project, logger;
    private final IThrowableProxy throwable;

    private Log(String thread, String level, String message, String project, String logger, IThrowableProxy throwable) {
        this.thread = thread;
        this.level = level;
        this.message = message;
        this.project = project;
        this.logger = logger;
        this.throwable = throwable;
    }

    public String getThread() {
        return thread;
    }

    public String getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public String getProject() {
        return project;
    }

    public String getLogger() {
        return logger;
    }

    public JSONObject toJson() throws Exception {
        JSONObject data = new JSONObject();
        data.put("thread", thread).put("level", level).put("message", message).put("project", project).put("logger", logger);
        if (throwable != null) {
            JSONObject exceptionData = new JSONObject();
            if (throwable.getMessage() != null && !throwable.getMessage().trim().isEmpty()) {
                exceptionData.put("message", throwable.getMessage());
            }
            if (throwable.getStackTraceElementProxyArray() != null) {
                exceptionData.put("trace",
                        Arrays.stream(throwable.getStackTraceElementProxyArray())
                                .map(StackTraceElementProxy::getStackTraceElement)
                                .filter(Objects::nonNull)
                                .limit(10)
                                .map(StackTraceElement::toString)
                                .toArray(String[]::new)
                );
            }
            if (exceptionData.length() > 0) {
                data.put("exception", exceptionData);
            }
        }
        return data;
    }

    public static class LogBuilder {

        private final String project;
        private String thread, level, message, logger;
        private IThrowableProxy throwable;

        public LogBuilder(String project) {
            this.project = project;
        }

        public LogBuilder setThread(String thread) {
            this.thread = thread;
            return this;
        }

        public LogBuilder setLevel(String level) {
            this.level = level;
            return this;
        }

        public LogBuilder setMessage(String message) {
            this.message = message;
            return this;
        }

        public LogBuilder setLogger(String logger) {
            this.logger = logger;
            return this;
        }

        public LogBuilder setThrowable(IThrowableProxy throwable) {
            this.throwable = throwable;
            return this;
        }

        public Log build() {
            Log log = new Log(thread, level, message, project, logger, throwable);
            thread = null;
            level = null;
            message = null;
            logger = null;
            throwable = null;
            return log;
        }
    }
}
