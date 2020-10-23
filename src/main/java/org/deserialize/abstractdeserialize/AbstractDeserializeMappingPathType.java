package org.deserialize.abstractdeserialize;

public enum AbstractDeserializeMappingPathType {
    FILE("file"), CLASSPATH("classpath"), URL("url");

    String name;

    AbstractDeserializeMappingPathType(String name) {
        this.name = name;
    }

    public String getName() {
        return name + ":";
    }

    public static AbstractDeserializeMappingPathType getValue(String name) {

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Parameter 'name' cannot be null or empty");
        }

        if ("file".equalsIgnoreCase(name)) {
            return FILE;
        }

        if ("classpath".equalsIgnoreCase(name)) {
            return CLASSPATH;
        }

        if ("url".equalsIgnoreCase(name)) {
            return URL;
        }

        // TODO create a custom exception and insert possibilities values
        throw new RuntimeException(String.format("No mapping path type find for input '%s'", name));
    }
}
