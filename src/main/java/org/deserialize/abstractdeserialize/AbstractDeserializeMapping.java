package org.deserialize.abstractdeserialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.deserialize.exception.InvalidCastException;
import org.deserialize.exception.InvalidPropertyException;
import org.deserialize.exception.UnhandledMethodException;
import org.deserialize.utils.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.time.*;
import java.util.*;

import static org.deserialize.utils.StringUtils.defaultIfBlank;
import static org.deserialize.utils.StringUtils.isNotBlank;

// TODO Gesytione di Array di array di array ( o liste di liste di liste )
public abstract class AbstractDeserializeMapping<T> extends StdDeserializer<T> {

    private static Logger logger = LoggerFactory.getLogger(AbstractDeserializeMapping.class);
    private static ObjectMapper mapper = new ObjectMapper();

    private Class<T> klass;
    private JsonNode mapping;
    private LoggerLevel loggerLevel;
    private Boolean ignoreUnmappedProperties;
    private Boolean ignoreUnknownProperties;

    /**
     * standard class' costructors
     */
    protected AbstractDeserializeMapping(Class<?> vc) {
        super(vc);
    }

    protected AbstractDeserializeMapping(JavaType valueType) {
        super(valueType);
    }

    protected AbstractDeserializeMapping(StdDeserializer<?> src) {
        super(src);
    }

    /**
     * custom class' costructor
     */
    protected AbstractDeserializeMapping(Class<T> klass, String mappingFilePath, AbstractDeserializeMappingPathType mappingPathType) throws IOException {
        super(klass);
        this.klass = klass;

        DeserializeProperties deserializeProperties = getProperties(klass.getSimpleName());
        loggerLevel = LoggerLevel.getLoggerLevel(defaultIfBlank(deserializeProperties.getLoggerLevel(), "info").trim());
        ignoreUnmappedProperties = Boolean.valueOf(defaultIfBlank(deserializeProperties.getIgnoreUnmappedProperties(), "false"));
        ignoreUnknownProperties = Boolean.valueOf(defaultIfBlank(deserializeProperties.getIgnoreUnknownProperties(), "true"));

        String fileExtension = ".json";

        // Configuration inside java class
        if (isNotBlank(mappingFilePath) && mappingPathType != null) {
            String resourceLocation = mappingPathType.getName() + mappingFilePath + (!mappingFilePath.endsWith(File.separator) ? File.separator : "") + (!mappingFilePath.endsWith(fileExtension) ? fileExtension : "");
            mapping = mapper.readTree(ResourceUtils.getFile(resourceLocation));

            if (isLogEnabled(LoggerLevel.DEBUG)) {
                logger.debug("Read file at path '{}'", resourceLocation);
            }
        } else {
            AbstractDeserializeMappingPathType pathType = AbstractDeserializeMappingPathType.getValue(defaultIfBlank(deserializeProperties.getPathType(), "classpath").trim());
            String path = defaultIfBlank(deserializeProperties.getPath());
            String prefix = defaultIfBlank(deserializeProperties.getPrefix());

            String resourceLocation = pathType.getName() + path + (!path.endsWith(File.separator) ? File.separator : "") + prefix + this.klass.getSimpleName().toLowerCase() + (!this.klass.getSimpleName().toLowerCase().endsWith(fileExtension) ? fileExtension : "");
            mapping = mapper.readTree(ResourceUtils.getFile(resourceLocation));

            if (isLogEnabled(LoggerLevel.DEBUG)) {
                logger.debug("Read file at path '{}'", resourceLocation);
            }
        }

        if (isLogEnabled(LoggerLevel.INFO)) {
            logger.info("Mapping created for class '{}'", this.klass.getSimpleName());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public T deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {

        if (mapping == null) {
            throw new RuntimeException(String.format("Invalid mapping properties for class %s", this.klass.getSimpleName()));
        }

        T obj;
        try {
            obj = klass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            if (isLogEnabled(LoggerLevel.ERROR)) {
                logger.error(e.getLocalizedMessage(), e);
            }
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }

        try {
            String inputNodeAsString = null;
            JsonNode inputNode = jsonParser.getCodec().readTree(jsonParser);

            // to avoid double cycles inside method 'toString'
            if (isLogEnabled(LoggerLevel.DEBUG)) {
                inputNodeAsString = inputNode.toString();
            }

            if (inputNodeAsString != null) {
                logger.debug("Start deserialization for class '{}' with json: '{}'", klass.getName(), inputNodeAsString);
            }

            obj = (T) deserialize(obj, inputNode, mapping);

            if (inputNodeAsString != null) {
                logger.debug("End deserialization for class '{}' with json: '{}'", klass.getName(), inputNodeAsString);
            }
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException | NoSuchFieldException | ParseException | InvalidPropertyException | InvalidCastException e) {
            if (isLogEnabled(LoggerLevel.ERROR)) {
                logger.error(e.getLocalizedMessage(), e);
            }
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }

        return obj;
    }

    /**
     *
     * @param root object to fill
     * @param inputValue json value to deserialize
     * @param mapping the mapping found in the configuration file
     * @return 'root' object in input
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     * @throws NoSuchFieldException
     * @throws ParseException
     * @throws InvalidPropertyException
     * @throws InvalidCastException
     * @throws IOException
     */
    private Object deserialize(Object root, JsonNode inputValue, JsonNode mapping) throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchFieldException, ParseException, InvalidPropertyException, InvalidCastException, IOException {

        for (Iterator<Map.Entry<String, JsonNode>> iterator = inputValue.fields(); iterator.hasNext();) {
            Map.Entry<String, JsonNode> currentNode = iterator.next();
            JsonNode mappingNode;

            // evaluate only not null properties
            if (!currentNode.getValue().isNull()) {
                mappingNode = mapping.get(currentNode.getKey());
                if (mappingNode != null) {

                    // to avoid useless cycle inside method 'toString'
                    if (isLogEnabled(LoggerLevel.DEBUG)) {
                        logger.debug("detect mapping '{}' for property '{}'", mappingNode.toString(), currentNode.getKey());
                    }

                    if (currentNode.getValue().isObject()) {
                        if (isLogEnabled(LoggerLevel.DEBUG)) {
                            logger.debug("deserialize property '{}' as object '{}'", currentNode.getKey(), currentNode.getValue().toString());
                        }

                        deserialize(root, currentNode.getValue(), mappingNode);
                    } else if (currentNode.getValue().isArray()) {

                        if (isLogEnabled(LoggerLevel.DEBUG)) {
                            logger.debug("deserialize property '{}' as array '{}'", currentNode.getKey(), currentNode.getValue().toString());
                        }

                        // initialize variable as mappingNode is a simple properties
                        String[] token = mappingNode.asText().split("\\.");
                        JsonNode mappingInnerObjectInArray = null;

                        if (mappingNode.isObject()) {
                            token = mappingNode.get("property").asText().split("\\.");
                            mappingInnerObjectInArray = mappingNode.get("mapping");

                            if (isLogEnabled(LoggerLevel.DEBUG)) {
                                logger.debug("mapping found for the inner object inside array '{}'", mappingInnerObjectInArray.toString());
                            }
                        }

                        if (mappingInnerObjectInArray == null && isLogEnabled(LoggerLevel.WARNING)) {
                            logger.warn("No mapping found for inner object inside array. The method 'getValueFromNode' is used'");
                        }

                        Object nestedObject = instanceNestedObject(root, token);
                        Class<?> propertyClass = instancePropertyDescriptor(token[token.length - 1], nestedObject.getClass()).getPropertyType();

                        if (isLogEnabled(LoggerLevel.DEBUG)) {
                            logger.debug("Identify class for inner object '{}'", propertyClass.getName());
                        }

                        // check if LocalDate object is serialized as array [ year, month, day ]
                        if (propertyClass.equals(LocalDate.class)) {
                            setter(nestedObject, token[token.length - 1], getValueFromNode(currentNode.getValue(), LocalDate.class));
                        }

                        // check if LocalDateTime object is serialized as array [ year, month, day, hours, minutes, seconds ]
                        if (propertyClass.equals(LocalDateTime.class)) {
                            setter(nestedObject, token[token.length - 1], getValueFromNode(currentNode.getValue(), LocalDateTime.class));
                        }

                        // check if property is an instance of array class
                        if (propertyClass.isArray()) {

                            Class<?> elementObjectClass = propertyClass.getComponentType();
                            Object array = Array.newInstance(elementObjectClass, currentNode.getValue().size());

                            if (isLogEnabled(LoggerLevel.DEBUG)) {
                                logger.debug("Detect array of '{}' with size '{}'", elementObjectClass.getSimpleName(), currentNode.getValue().size());
                            }

                            for (int i = 0; i < currentNode.getValue().size(); i++) {
                                JsonNode arrayNode = currentNode.getValue().get(i);
                                if (arrayNode.isArray()) {
                                    // TODO da provare gli array di array
                                    Object nestedArray = Array.newInstance(elementObjectClass, arrayNode.size());
                                    int j = 0;
                                    // TODO trovare una soluzione per fare il cast .... altrimenti non si riesce ad impostare il valore
                                    for (Iterator<JsonNode> it = arrayNode.elements(); it.hasNext();) {
//                                        JsonNode app = it.next();
//                                        if (app.isArray()) {
//                                            Array.set(nestedArray, j++, deserialize(root, it.next(), mapping));
//                                        } else if (app.isObject()) {
//                                            Array.set(nestedArray, j++, deserialize(root, it.next(), mapping));
//                                        } else {
//                                            Array.set(nestedArray, j++, getValueFromNode(arrayNode, elementObjectClass));
//                                        }
                                        Array.set(nestedArray, j++, deserialize(root, it.next(), mapping));
                                    }
                                    Array.set(array, i, nestedArray);
                                } else if (arrayNode.isObject()) {
                                    Array.set(array, i, deserialize(elementObjectClass.newInstance(), arrayNode, mappingInnerObjectInArray));
                                } else {
                                    Array.set(array, i, getValueFromNode(arrayNode, elementObjectClass));
                                }
                            }

                            setter(nestedObject, token[token.length - 1], array);
                        }

                        // check if the property is an instance of list class or set class
                        if (List.class.isAssignableFrom(propertyClass) || Set.class.isAssignableFrom(propertyClass)) {
                            Class<?> collectionClass = propertyClass;
                            Class<?> elementClass = getElementClassFromCollection(nestedObject, token[token.length - 1]);

                            if (Modifier.isAbstract(propertyClass.getModifiers()) || Modifier.isInterface(propertyClass.getModifiers())) {
                                if (List.class.isAssignableFrom(propertyClass)) {
                                    collectionClass = ArrayList.class;
                                } else if (Set.class.isAssignableFrom(propertyClass)) {
                                    collectionClass = HashSet.class;
                                }

                                if (isLogEnabled(LoggerLevel.WARNING)) {
                                    logger.warn("Collection class is abstract, so the default class is used '{}'", collectionClass.getSimpleName());
                                }
                            }

                            Collection collection = (Collection) collectionClass.newInstance();

                            if (isLogEnabled(LoggerLevel.DEBUG)) {
                                logger.debug("Detect collection '{}' with element class '{}' and size '{}'", collectionClass.getSimpleName(), elementClass.getSimpleName(), currentNode.getValue().size());
                            }

                            for (JsonNode jsonNode : currentNode.getValue()) {
                                if (jsonNode.isArray()) {
                                    // TODO trovare un meccanismo di deserializzazione di un array
                                } else if (jsonNode.isObject()) {
                                    collection.add(deserialize(elementClass.newInstance(), jsonNode, mappingInnerObjectInArray));
                                } else {
                                    collection.add(getValueFromNode(jsonNode, elementClass));
                                }
                            }

                            setter(nestedObject, token[token.length - 1], collection);
                        }
                    } else {
                        String[] token = mappingNode.asText().split("\\.");

                        if (isLogEnabled(LoggerLevel.DEBUG)) {
                            logger.debug("Mapping identify '{}' for the key '{}'", mappingNode.asText(), currentNode.getKey());
                        }

                        setter(instanceNestedObject(root, token), token[token.length - 1], currentNode.getValue());
                    }
                } else if (!ignoreUnmappedProperties) {
                    // it can be better
                    try {
                        if (currentNode.getValue().isObject()) {
                            // TODO questo metodo non è corretto perchè vengono chiamati i setter della root, se la proprietà è dentro un oggetto non della root, il metodo non funziona
                            setter(root, currentNode.getKey(), mapper.readValue(currentNode.getValue().toString(), newInstance(root, currentNode.getKey()).getClass()));
                        } else {
                            setter(root, currentNode.getKey(), currentNode.getValue());
                        }
                    } catch (InvalidPropertyException e) {
                        if (!ignoreUnknownProperties) {
                            throw e;
                        }
                    }
                }
            }
        }

        return root;
    }

    protected final void setter(Object object, String fieldName, JsonNode nodeValue) throws InvocationTargetException, IllegalAccessException, NoSuchFieldException, ParseException, InvalidPropertyException, InvalidCastException {
        instancePropertyDescriptor(fieldName, object.getClass()).getWriteMethod().invoke(object, getValueFromNode(nodeValue, object.getClass().getDeclaredField(fieldName).getType()));
    }

    protected final void setter(Object object, String fieldName, Object value) throws InvocationTargetException, IllegalAccessException, InvalidPropertyException {
        instancePropertyDescriptor(fieldName, object.getClass()).getWriteMethod().invoke(object, value);
    }

    protected final Object getter(Object object, String fieldName) throws InvocationTargetException, IllegalAccessException, InvalidPropertyException {
        return instancePropertyDescriptor(fieldName, object.getClass()).getReadMethod().invoke(object);
    }

    protected final Object newInstance(Object object, String fieldName) throws IllegalAccessException, InstantiationException, InvalidPropertyException {
        // can replace with: return object.getClass().getDeclaredField(fieldName).getType().newInstance();
        return instancePropertyDescriptor(fieldName, object.getClass()).getPropertyType().newInstance();
    }

    protected static Class<?> getElementClassFromCollection(Object object, String collectionFieldName) throws InvalidPropertyException {
        try {
            return (Class<?>) ((ParameterizedType) object.getClass().getDeclaredField(collectionFieldName).getGenericType()).getActualTypeArguments()[0];
        } catch (ClassCastException e) {
            return Object.class;
        } catch (NoSuchFieldException fieldException) {
            throw new InvalidPropertyException(String.format("Field '%s' not found", collectionFieldName));
        }
    }

    private PropertyDescriptor instancePropertyDescriptor(String fieldName, Class<?> objectClass) throws InvalidPropertyException {
        try {
            return new PropertyDescriptor(fieldName, objectClass);
        } catch (IntrospectionException e) {
            throw new InvalidPropertyException(String.format("Method or field '%s' not found", fieldName));
        }
    }

    private Object instanceNestedObject(Object root, String[] token) throws IllegalAccessException, InstantiationException, InvocationTargetException, InvalidPropertyException {

        Object obj = root;

        // looking for nested object
        for (int i = 0; i < token.length - 1; i++) {
            Object child = getter(obj, token[i]);
            if (child == null) {
                child = newInstance(obj, token[i]);
                setter(obj, token[i], child);
            }
            obj = child;
        }

        return obj;
    }

    // the method return exception about input and class, but not about the property
    private Object getValueFromNode(JsonNode jsonNode, Class<?> classToCast) throws ParseException, InvalidCastException {

        if (jsonNode.isObject()) {
            throw new InvalidCastException(String.format("Input node is an object '%s'. Cannot be cast to class '%s'", jsonNode.toString(), classToCast.getName()));
        }

        if (jsonNode.isNull()) {
            return null;
        }

        String stringValue = jsonNode.asText();

        // child classes can use this method to add a custom evaluation for current node
        try {
            return getCustomValueFromNode(jsonNode, classToCast);
        } catch (UnhandledMethodException e) {
            if (isLogEnabled(LoggerLevel.DEBUG)) {
                logger.debug(e.getLocalizedMessage());
            }
        }

        if (classToCast.equals(Character.class)) {
            if (stringValue.length() == 0) {
                return Character.MIN_VALUE;
            }
            return stringValue.charAt(0);
        }

        if (classToCast.equals(Short.class)) {
            return Short.valueOf(stringValue);
        }

        if (classToCast.equals(Integer.class)) {
            return Integer.valueOf(stringValue);
        }

        if (classToCast.equals(Long.class)) {
            return Long.valueOf(stringValue);
        }

        if (classToCast.equals(Float.class)) {
            return Float.valueOf(stringValue);
        }

        if (classToCast.equals(Double.class)) {
            return Double.valueOf(stringValue);
        }

        if (classToCast.equals(Boolean.class)) {
            return Boolean.valueOf(stringValue);
        }

        if (classToCast.equals(BigInteger.class)) {
            return new BigInteger(stringValue);
        }

        if (classToCast.equals(BigDecimal.class)) {
            return new BigDecimal(stringValue);
        }

        if (classToCast.equals(UUID.class)) {
            return UUID.fromString(stringValue);
        }

        if (classToCast.equals(Date.class)) {
            try {
                return TimeUtils.getDate(Long.valueOf(stringValue));
            } catch (NumberFormatException e) {
                // do nothing
            }
            return TimeUtils.getDate(stringValue);
        }

        if (classToCast.equals(Instant.class)) {
            try {
                return TimeUtils.getInstant(Long.valueOf(stringValue.replace(".", "")));
            } catch (NumberFormatException e) {
                // do nothing
            }
            return TimeUtils.getInstant(stringValue);
        }

        if (classToCast.equals(Calendar.class)) {
            try {
                return TimeUtils.getCalendar(Long.valueOf(stringValue));
            } catch (NumberFormatException e) {
                // do nothing
            }
            return TimeUtils.getCalendar(stringValue);
        }

        if (classToCast.equals(LocalDate.class)) {
            try {
                return TimeUtils.getLocalDate(Long.valueOf(stringValue));
            } catch (NumberFormatException e) {
                // do nothing
            }
            if (jsonNode.isArray()) {
                return LocalDate.of(jsonNode.get(0).asInt(1970), jsonNode.get(1).asInt(1), jsonNode.get(2).asInt(1));
            }
            return TimeUtils.getLocalDate(stringValue);
        }

        if (classToCast.equals(LocalDateTime.class)) {
            try {
                return TimeUtils.getLocalDateTime(Long.valueOf(stringValue));
            } catch (NumberFormatException e) {
                // do nothing
            }
            if (jsonNode.isArray()) {
                return LocalDateTime.of(jsonNode.get(0).asInt(1970), jsonNode.get(1).asInt(1), jsonNode.get(2).asInt(1), jsonNode.get(3).asInt(0), jsonNode.get(4).asInt(0), jsonNode.get(5).asInt(0));
            }
            return TimeUtils.getLocalDateTime(stringValue);
        }

        if (classToCast.equals(ZonedDateTime.class)) {
            try {
                return ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.valueOf(stringValue)), ZoneOffset.UTC);
            } catch (NumberFormatException e) {
                // do nothing
            }
            return TimeUtils.getZonedDateTime(stringValue);
        }

        return stringValue;
    }

    /**
     * subclasses utility's methods
     */
    protected Object getCustomValueFromNode(JsonNode jsonNode, Class<?> classToCast) throws UnhandledMethodException {
        throw new UnhandledMethodException(String.format("Protected method 'getCustomValueFromNode' not handling for class '%s'", klass.getName()));
    }

    protected final boolean isLogEnabled(LoggerLevel loggerLevel) {
        return loggerLevel.getValue() <= this.loggerLevel.getValue();
    }

    /**
     * private methods
     */
    private DeserializeProperties getProperties(String className) {

        Assert.notNull(className, "className cannot be blank");

        DeserializeProperties properties = new DeserializeProperties();
        Properties applicationProperties = new Properties();

        try {
            applicationProperties.load(new FileInputStream(ResourceUtils.getFile("classpath:application.properties")));
        } catch (IOException e) {
            if (isLogEnabled(LoggerLevel.ERROR)) {
                logger.error(e.getLocalizedMessage(), e);
            }
        }

        properties.setPath(defaultIfBlank(applicationProperties.getProperty("abstract-deserialize.resource." + className.toLowerCase() + ".path"), applicationProperties.getProperty("abstract-deserialize.path")));
        properties.setPrefix(defaultIfBlank(applicationProperties.getProperty("abstract-deserialize.resource." + className.toLowerCase() + ".prefix"), applicationProperties.getProperty("abstract-deserialize.prefix")));
        properties.setPathType(defaultIfBlank(applicationProperties.getProperty("abstract-deserialize.resource." + className.toLowerCase() + ".path-type"), applicationProperties.getProperty("abstract-deserialize.path-type")));
        properties.setLoggerLevel(defaultIfBlank(applicationProperties.getProperty("abstract-deserialize.resource." + className.toLowerCase() + ".logger-level"), applicationProperties.getProperty("abstract-deserialize.logger-level")));
        properties.setIgnoreUnmappedProperties(defaultIfBlank(applicationProperties.getProperty("abstract-deserialize.resource." + className.toLowerCase() + ".ignore-unmapped-properties"), applicationProperties.getProperty("abstract-deserialize.ignore-unmapped-properties")));
        properties.setIgnoreUnknownProperties(defaultIfBlank(applicationProperties.getProperty("abstract-deserialize.resource." + className.toLowerCase() + ".ignore-unknown-properties"), applicationProperties.getProperty("abstract-deserialize.ignore-unknown-properties")));

        return properties;
    }

    private class DeserializeProperties {

        private String path;
        private String pathType;
        private String prefix;
        private String loggerLevel;
        private String ignoreUnmappedProperties;
        private String ignoreUnknownProperties;

        private String getPath() {
            return path;
        }

        private void setPath(String path) {
            this.path = path;
        }

        private String getPathType() {
            return pathType;
        }

        private void setPathType(String pathType) {
            this.pathType = pathType;
        }

        private String getPrefix() {
            return prefix;
        }

        private void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        private String getLoggerLevel() {
            return loggerLevel;
        }

        private void setLoggerLevel(String loggerLevel) {
            this.loggerLevel = loggerLevel;
        }

        private String getIgnoreUnmappedProperties() {
            return ignoreUnmappedProperties;
        }

        private void setIgnoreUnmappedProperties(String ignoreUnmappedProperties) {
            this.ignoreUnmappedProperties = ignoreUnmappedProperties;
        }

        private String getIgnoreUnknownProperties() {
            return ignoreUnknownProperties;
        }

        private void setIgnoreUnknownProperties(String ignoreUnknownProperties) {
            this.ignoreUnknownProperties = ignoreUnknownProperties;
        }
    }

    public static void main(String[] args) {
        Integer[][] array = new Integer[3][];
        Integer[] first = new Integer[]{0, 1, 3};
        Array.set(array, 0, first);
        System.out.println("riuscito");
    }
}
