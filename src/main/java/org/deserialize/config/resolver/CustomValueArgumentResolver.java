package org.deserialize.config.resolver;

import com.usermanager.exceptions.CustomException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import springfox.documentation.service.ParameterType;

import javax.servlet.http.HttpServletRequest;

@Component
public class CustomValueArgumentResolver implements HandlerMethodArgumentResolver {
  
  @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        return methodParameter.hasParameterAnnotation(CustomValue.class);
    }

    @Override
    public Object resolveArgument(MethodParameter methodParameter, ModelAndViewContainer modelAndViewContainer, NativeWebRequest nativeWebRequest, WebDataBinderFactory webDataBinderFactory) throws Exception {
        HttpServletRequest request = (HttpServletRequest) nativeWebRequest.getNativeRequest();
        UniqueId parameterAnnotation = methodParameter.getParameterAnnotation(CustomValue.class);

        Object customValue;
        if (ParameterType.HEADER.equals(parameterAnnotation.type())) {
            customValue = request.getHeader(parameterAnnotation.value());
        } else {
            customValue = request.getParameter(parameterAnnotation.value());
        }

        if (customValue == null && parameterAnnotation.required()) {
            throw new CustomException(HttpStatus.BAD_REQUEST.value(), "missing required parameter: " + parameterAnnotation.name(), null);
        }

        return customValue;
    }
}
