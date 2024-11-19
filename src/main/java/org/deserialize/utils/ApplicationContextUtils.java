package com.enel.eic.commons.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ApplicationContextUtils {

    private static ApplicationContext applicationContext;
    private static ConfigurableBeanFactory beanFactory;
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String RELEASE_VERSION = "release_version";

    public static void setApplicationContext(ApplicationContext applicationContext) {
        ApplicationContextUtils.applicationContext = applicationContext;
    }

    public static void setConfigurableBeanFactory(ConfigurableBeanFactory beanFactory) {
         ApplicationContextUtils.beanFactory = beanFactory;
    }

    public static String getTransactionId() {
        return MDC.get(LabelUtils.TRANSACTION_ID);
    }

    public static <T> T getBean(Class<T> klass) {
        return applicationContext.getBean(klass);
    }

    public static <T> T getBean(String beanName, Class<T> klass) {
        return applicationContext.getBean(beanName, klass);
    }

    public static Object evalExpression(String expression) {
        BeanExpressionResolver beanExpressionResolver = beanFactory.getBeanExpressionResolver();
        if (beanExpressionResolver == null) {
            return null;
        }

        String expressionWithSubstitutedVariables = beanFactory.resolveEmbeddedValue(expression);
        return beanExpressionResolver.evaluate(expressionWithSubstitutedVariables, new BeanExpressionContext(beanFactory, null));
    }

    public static String getExceptionMessage(Throwable exception) {
        return StringUtils.defaultIfBlank(exception.getLocalizedMessage(), exception.getMessage());
    }

    public static String getExceptionStackTrace(Throwable exception) {
        if (exception == null) {
            return null;
        }
        String exceptionHeader = exception.getClass().getName() + ": " + StringUtils.defaultIfBlank(exception.getLocalizedMessage(), exception.getMessage());
        return exceptionHeader + LabelUtils.AWS_CLOUD_WATCH_LINE_SEPARATOR + Arrays.stream(exception.getStackTrace())
                .map(stackTraceelement -> "\tat " + stackTraceelement.toString())
                .collect(Collectors.joining(LabelUtils.AWS_CLOUD_WATCH_LINE_SEPARATOR));
    }

    public static String replaceContextLineSeparator(String input) {
        if (input == null) {
            return null;
        }

        return input.replace("\n", LabelUtils.AWS_CLOUD_WATCH_LINE_SEPARATOR);
    }

    public static String getReleaseVersion() {
        try {
            JsonNode node = mapper.readValue(ResourceUtils.getFile("./automation_conf.json"), JsonNode.class);
            if (node.get(RELEASE_VERSION) == null || node.get(RELEASE_VERSION).isNull()) {
                return null;
            }

            return node.get(RELEASE_VERSION).asText();
        } catch (IOException e) {
            return null;
        }
    }
}
