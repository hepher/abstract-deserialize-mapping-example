package com.enelx.bfw.framework.aspect;

import com.enelx.bfw.framework.resolver.TransactionId;
import com.enelx.bfw.framework.util.LabelUtils;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

@Getter
public class JoinPointDetail {

    private String transactionId;
    private final Object[] args;
    private final String method;
    private String klass;
    private final String[] parameterNames;
    private final MethodSignature methodSignature;
    private final Map<String, Parameter> parameterMap;

    record Parameter(Class<?> klass, Object value){}

    public JoinPointDetail(ProceedingJoinPoint joinPoint) {
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

        method = joinPoint.getSignature().getName();
        args = joinPoint.getArgs();

        try {
            String[] splitPackage = joinPoint.getSignature().getDeclaringTypeName().split("\\.");
            klass = splitPackage[splitPackage.length - 1];
        } catch (Exception e) {
            klass = joinPoint.getTarget().getClass().getSimpleName();
        }

        methodSignature = (MethodSignature) joinPoint.getSignature();
        parameterNames = methodSignature.getParameterNames();

        AtomicInteger integer = new AtomicInteger(0);
        parameterMap = Arrays.stream(joinPoint.getArgs()).collect(HashMap::new, (map, param) -> {

            int index = integer.getAndIncrement();

            if (checkTransactionAnnotationFunction.apply(methodSignature, index)) {
                transactionId = param.toString();
            }

            // avoid IndexOutOfBoundsException
            Class<?> parameterClass = (Class<?>) Array.get(methodSignature.getParameterTypes(), index);
            if (index < parameterNames.length) {
                map.put(parameterNames[index], new Parameter(parameterClass, param));
            } else {
                map.put(index + "", new Parameter(parameterClass, param));
            }
        }, HashMap::putAll);

        if (transactionId == null) {
            transactionId = StringUtils.defaultIfBlank(MDC.get(LabelUtils.TRANSACTION_ID), UUID.randomUUID().toString());
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
