package com.enelx.bfw.framework.aspect;

import com.enelx.bfw.framework.resolver.TransactionId;
import com.enelx.bfw.framework.util.LabelUtils;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

@Getter
public class JoinPointDetail {

    private String transactionId;
    private final Object[] args;
    private final String method;
    private String methodKlass;
    private final String executionKlass;
    private String parentKlass;
    private String parentMethod;
    private final String[] parameterNames;
    private final MethodSignature methodSignature;
    private final Map<String, Parameter> parameterMap;
    private Object body;

    record Parameter(Class<?> klass, Object value){}

    public JoinPointDetail(ProceedingJoinPoint joinPoint, String transactionId) {
        BiFunction<MethodSignature, Integer, Boolean> checkTransactionAnnotationFunction = (methodSignature, i) -> {
            if (i < methodSignature.getMethod().getParameterAnnotations().length) {
                for (Annotation annotation : methodSignature.getMethod().getParameterAnnotations()[i]) {
                    if (annotation.annotationType().equals(TransactionId.class)) {
                        return true;
                    }
                }
            }

            return false;
        };

        BiFunction<MethodSignature, Integer, Boolean> checkRequestBodyAnnotationFunction = (methodSignature, i) -> {
            if (i < methodSignature.getMethod().getParameterAnnotations().length) {
                for (Annotation annotation : methodSignature.getMethod().getParameterAnnotations()[i]) {
                    if (annotation.annotationType().equals(RequestBody.class)) {
                        return true;
                    }
                }
            }

            return false;
        };

        Function<String, String> extractClassFromPackageFunction = (pack -> {
            if (pack == null) {
                return null;
            }

            try {
                String[] splitPackage = pack.split("\\.");
                return splitPackage[splitPackage.length - 1];
            } catch (Exception e) {
                return null;
            }
        });

        method = joinPoint.getSignature().getName();
        args = joinPoint.getArgs();

        executionKlass = joinPoint.getTarget().getClass().getSimpleName();
        methodKlass = extractClassFromPackageFunction.apply(joinPoint.getSignature().getDeclaringTypeName());
        if (methodKlass == null) {
            methodKlass = joinPoint.getTarget().getClass().getSimpleName();
        }

        List<StackTraceElement> filteredStack = Arrays.stream(Thread.currentThread().getStackTrace())
            .filter(stackTraceElement ->
                stackTraceElement.getClassName().contains(joinPoint.getTarget().getClass().getPackageName()) &&
                stackTraceElement.getFileName() != null &&
                !stackTraceElement.getFileName().equals("<generated>")) // ignore cglib spring proxy
            .toList();

        if (!filteredStack.isEmpty()) {
            StackTraceElement traceElement = filteredStack.get(0);
            parentKlass = extractClassFromPackageFunction.apply(traceElement.getClassName());
            parentMethod = traceElement.getMethodName();
        }

        methodSignature = (MethodSignature) joinPoint.getSignature();
        parameterNames = methodSignature.getParameterNames();

        AtomicInteger integer = new AtomicInteger(0);
        parameterMap = Arrays.stream(joinPoint.getArgs()).collect(HashMap::new, (map, param) -> {

            int index = integer.getAndIncrement();

            if (checkTransactionAnnotationFunction.apply(methodSignature, index)) {
                this.transactionId = param.toString();
            }

            if (checkRequestBodyAnnotationFunction.apply(methodSignature, index)) {
                this.body = param;
            }

            // avoid IndexOutOfBoundsException
            Class<?> parameterClass = (Class<?>) Array.get(methodSignature.getParameterTypes(), index);
            if (index < parameterNames.length) {
                map.put(parameterNames[index], new Parameter(parameterClass, param));
            } else {
                map.put(index + "", new Parameter(parameterClass, param));
            }
        }, HashMap::putAll);

        if (this.transactionId == null) {
            this.transactionId = transactionId;
        }

        if (this.transactionId == null) {
            this.transactionId = StringUtils.defaultIfBlank(MDC.get(LabelUtils.TRANSACTION_ID), UUID.randomUUID().toString());
        }
    }

    public Map<String, Object> getSimpleParameterMap() {
        Map<String, Object> simpleParameterMap = new HashMap<>();
        this.parameterMap.forEach((name, parameter) -> simpleParameterMap.put(name, parameter.value()));

        return simpleParameterMap;
    }

    public List<String> getParameterListAsString() {
        List<String> result = new ArrayList<>();
        parameterMap.forEach((name, parameter) -> {
            if (NumberUtils.isDigits(name)) {
                result.add(parameter.value.toString());
            } else {
                result.add(StringUtils.join(name, "=", parameter.value != null ? parameter.value.toString() : null));
            }
        });

        return result;
    }
}
