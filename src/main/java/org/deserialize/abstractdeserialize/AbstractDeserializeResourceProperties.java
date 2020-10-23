package org.deserialize.abstractdeserialize;

public class AbstractDeserializeResourceProperties {
    private String path;
    private String pathType;
    private String prefix;
    private String loggerLevel;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPathType() {
        return pathType;
    }

    public void setPathType(String pathType) {
        this.pathType = pathType;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getLoggerLevel() {
        return loggerLevel;
    }

    public void setLoggerLevel(String loggerLevel) {
        this.loggerLevel = loggerLevel;
    }
}