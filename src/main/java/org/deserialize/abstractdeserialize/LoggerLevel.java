package org.deserialize.abstractdeserialize;

public enum LoggerLevel {
    ERROR(0), INFO(1), WARNING(2), DEBUG(2);

    int value;

    LoggerLevel(int value) {
        this.value = value;
    }

    int getValue() {
        return value;
    }

    public static LoggerLevel getLoggerLevel(String value) {

        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Parameter 'name' cannot be null or empty");
        }

        switch (value) {
            case "error":
            case "ERROR":
                return ERROR;
            case "info":
            case "INFO":
                return INFO;
            case "warning":
            case "WARNING":
                return WARNING;
            case "debug":
            case "DEBUG":
                return DEBUG;
        }

        throw new RuntimeException(String.format("No logger level find for the value '%s'", value));
    }
}
