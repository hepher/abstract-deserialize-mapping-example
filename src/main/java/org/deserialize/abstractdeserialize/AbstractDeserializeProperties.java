package org.deserialize.abstractdeserialize;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties("abstract-deserialize")
public class AbstractDeserializeProperties {

    private String path;
    private String pathType;
    private String prefix;
    private String loggerLevel;
    private Map<String, AbstractDeserializeResourceProperties> resource = new HashMap<>();

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

    /** getters to quick access to resource properties */
    // use empty class for default to avoid any sort of exception ( to analize after insert log )
    public String getResourcePath(String resourceName) {
        return resource.getOrDefault(resourceName, new AbstractDeserializeResourceProperties()).getPath();
    }

    public String getResourcePathType(String resourceName) {
        return resource.getOrDefault(resourceName, new AbstractDeserializeResourceProperties()).getPathType();
    }

    public String getResourcePrefix(String resourceName) {
        return resource.getOrDefault(resourceName, new AbstractDeserializeResourceProperties()).getPrefix();
    }

    public String getResourceLoggerLevel(String resourceName) {
        return resource.getOrDefault(resourceName, new AbstractDeserializeResourceProperties()).getLoggerLevel();
    }

    public Map<String, AbstractDeserializeResourceProperties> getResource() {
        return resource;
    }
}
