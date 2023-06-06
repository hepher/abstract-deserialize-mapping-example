package org.deserialize.config.resolver;

import org.springframework.stereotype.Component;
import springfox.documentation.service.ResolvedMethodParameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.ParameterBuilderPlugin;
import springfox.documentation.spi.service.contexts.ParameterContext;

import java.util.Optional;

@Component
public class CustomValueSwaggerParameter implements ParameterBuilderPlugin {
  
  @Override
    public void apply(ParameterContext parameterContext) {
        ResolvedMethodParameter methodParameter = parameterContext.resolvedMethodParameter();
        Optional<CustomValue> requestParam = methodParameter.findAnnotation(CustomValue.class);
        if (requestParam.isPresent()) {
            parameterContext.requestParameterBuilder().in(requestParam.get().type());
            parameterContext.requestParameterBuilder().description(requestParam.get().description());
            parameterContext.requestParameterBuilder().required(requestParam.get().required());
            parameterContext.requestParameterBuilder().name(requestParam.get().name());
        }
    }

    @Override
    public boolean supports(DocumentationType documentationType) {
        return true;
    }
}
