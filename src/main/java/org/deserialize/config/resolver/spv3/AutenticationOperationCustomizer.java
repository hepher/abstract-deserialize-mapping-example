package com.enelx.bfw.framework.resolver;

import com.enelx.bfw.framework.security.validation.Authenticated;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

@Component
public class AuthenticatedOperationCustomizer implements OperationCustomizer {

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {

        MasheryUniqueId masheryUniqueId = AnnotationUtils.synthesizeAnnotation(MasheryUniqueId.class);

        if (handlerMethod.hasMethodAnnotation(Authenticated.class)) {
            Parameter parameter = new Parameter();
            parameter.setName(masheryUniqueId.value());
            parameter.setRequired(true);
            parameter.setIn(masheryUniqueId.type().getValue());
            parameter.setDescription(masheryUniqueId.description());
            operation.addParametersItem(parameter);
        }

        return operation;
    }
}
