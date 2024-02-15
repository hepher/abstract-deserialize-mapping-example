package com.enelx.bfw.framework.aspect;

import com.enelx.bfw.framework.resolver.TransactionId;
import com.enelx.bfw.framework.util.ApplicationContextUtils;
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

	record Parameter(Class<?> klass, Object value) {
	}

	BiPredicate<MethodSignature, Integer> checkTransactionAnnotationFunction = (mf, i) -> {
		if (i < mf.getMethod().getParameterAnnotations().length) {
			for (Annotation annotation : mf.getMethod().getParameterAnnotations()[i]) {
				if (annotation.annotationType().equals(TransactionId.class)) {
					return true;
				}
			}
		}
		return false;
	};

	BiPredicate<MethodSignature, Integer> checkRequestBodyAnnotationFunction = (mf, i) -> {
		if (i < mf.getMethod().getParameterAnnotations().length) {
			for (Annotation annotation : mf.getMethod().getParameterAnnotations()[i]) {
				if (annotation.annotationType().equals(RequestBody.class)) {
					return true;
				}
			}
		}
		return false;
	};

	UnaryOperator<String> extractClassFromPackageFunction = (pack -> {
		if (pack != null) {
			String[] splitPackage = pack.split("\\.");
			return splitPackage[splitPackage.length - 1];
		}
		return null;
	});

	public JoinPointDetail(ProceedingJoinPoint joinPoint, String transactionId) {
		method = joinPoint.getSignature().getName();
		args = joinPoint.getArgs();

		executionKlass = joinPoint.getTarget().getClass().getSimpleName();
		methodKlass = extractClassFromPackageFunction.apply(joinPoint.getSignature().getDeclaringTypeName());
		if (methodKlass == null) {
			methodKlass = joinPoint.getTarget().getClass().getSimpleName();
		}

		List<StackTraceElement> filteredStack = Arrays.stream(Thread.currentThread().getStackTrace())
				.filter(stackTraceElement ->
						stackTraceElement.getClassName().contains(joinPoint.getTarget().getClass().getPackageName())
						&& stackTraceElement.getFileName() != null
						&& !stackTraceElement.getFileName().equals("<generated>")) // ignore cglib spring proxy
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

			if (checkTransactionAnnotationFunction.test(methodSignature, index)) {
				this.transactionId = param.toString();
			}

			if (checkRequestBodyAnnotationFunction.test(methodSignature, index)) {
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
			this.transactionId = StringUtils.defaultIfBlank(ApplicationContextUtils.getTransactionId(), UUID.randomUUID().toString());
		}
	}

	public Map<String, Object> getSimpleParameterMap() {
		return parameterMap.entrySet().stream().collect(HashMap::new, (map, parameterEntry) -> map.put(parameterEntry.getKey(), parameterEntry.getValue()), HashMap::putAll);
	}

	public List<String> getParameterListAsString() {		
		return parameterMap.entrySet().stream()
				.map(entry -> NumberUtils.isDigits(entry.getKey()) ? entry.getValue().toString() : StringUtils.join(entry.getKey(), "=", String.valueOf(entry.getValue().value)))
				.toList();
	}
}
