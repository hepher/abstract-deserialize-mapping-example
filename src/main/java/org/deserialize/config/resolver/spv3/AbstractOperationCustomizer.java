package com.enelx.bfw.framework.resolver;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.MethodParameter;
import org.springframework.web.method.HandlerMethod;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.function.Function;

public abstract class AbstractOperationCustomizer<A extends Annotation> implements OperationCustomizer {

    protected final Class<A> klass;
    protected final Function<A, OperationDetail> annotationParseFunction;

    protected AbstractOperationCustomizer(Class<A> klass, Function<A, OperationDetail> transform) {
        this.klass = klass;
        this.annotationParseFunction = transform;
    }

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {

        String parameterName = null;
        A annotation = null;
        String bodyParameterName = null;
        for (MethodParameter methodParameter : Arrays.stream(handlerMethod.getMethodParameters()).toList()) {
            if (methodParameter.hasParameterAnnotation(klass)) {
                parameterName = methodParameter.getParameter().getName();
                annotation = methodParameter.getParameter().getAnnotation(klass);
            }

            if (methodParameter.hasParameterAnnotation(RequestBody.class)) {
                bodyParameterName = methodParameter.getParameter().getName();
            }
        }

        String finalParameterName = parameterName;
        if (annotation != null) {
            OperationDetail detail = annotationParseFunction.apply(annotation);
            if (detail != null) {

                if (operation.getParameters() != null && detail.getIn() != null && !detail.getIn().getValue().equals(ParameterType.BODY.getValue())) {
                    Parameter parameter =  operation.getParameters().stream()
                            .filter(param -> finalParameterName.equals(param.getName()))
                            .findFirst()
                            .orElse(null);

                    if (parameter == null) {
                        parameter = new Parameter();
                        operation.addParametersItem(parameter);
                    }

                    parameter.setName(detail.getName());
                    parameter.setIn(detail.getIn() != null ? detail.getIn().getValue() : null);
                    parameter.setRequired(detail.isRequired());
                    parameter.setDescription(StringUtils.isNotBlank(detail.getDescription()) ? detail.getDescription() : null);
                    parameter.setExample(StringUtils.isNotBlank(detail.getExample()) ? detail.getExample() : null);
                }

                io.swagger.v3.oas.models.parameters.RequestBody requestBody = operation.getRequestBody();
                if (requestBody != null && bodyParameterName != null) {
                    Schema<?> bodySchema = requestBody.getContent().get("application/json").getSchema();
                    // the schema is replaced with schema of @RequestBody parameter to avoid scenario ("refreshToken": { "refreshToken": {...}})
                    if (bodySchema.getProperties() != null && bodySchema.getProperties().containsKey(bodyParameterName)) {
                        Schema<?> bodyParameterSchema = bodySchema.getProperties().get(bodyParameterName);
                        requestBody.getContent().get("application/json").setSchema(bodyParameterSchema);
                    }
                }
            }
        }

        return operation;
    }
}
