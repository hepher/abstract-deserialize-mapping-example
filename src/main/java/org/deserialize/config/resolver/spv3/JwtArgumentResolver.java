package com.enel.eic.commons.resolver;


import com.enel.eic.commons.exception.CommonsException;
import com.enel.eic.commons.util.RequestParameterUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class JwtArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        return methodParameter.hasParameterAnnotation(Jwt.class);
    }

    @Override
    public Object resolveArgument(MethodParameter methodParameter, ModelAndViewContainer mavContainer, NativeWebRequest nativeWebRequest, WebDataBinderFactory binderFactory) throws Exception {

        HttpServletRequest request = (HttpServletRequest) nativeWebRequest.getNativeRequest();
        Jwt parameterAnnotation = methodParameter.getParameterAnnotation(Jwt.class);

        String encodedJwt = null;
        if (parameterAnnotation != null) {
            String authorizationValue = (String) RequestParameterUtils.getValueFromRequest(request, parameterAnnotation.type(), parameterAnnotation.value());

            if (authorizationValue == null) {
                if (parameterAnnotation.required()) {
                    throw new CommonsException("Missing required jwt");
                }

                return null;
            }

            encodedJwt = authorizationValue.replaceFirst("^" + parameterAnnotation.tokenType().getValue() + " ", "");

            if (StringUtils.isBlank(encodedJwt)) {
                if (parameterAnnotation.required()) {
                    throw new CommonsException("Missing required jwt");
                }

                return null;
            }
        }

        Class<?> parameterType = methodParameter.getParameterType();
        if (String.class.isAssignableFrom(parameterType)) {
            return encodedJwt;
        }

        if (!com.enel.eic.commons.security.jwt.Jwt.class.isAssignableFrom(parameterType)) {
            throw new CommonsException("Invalid method class");
        }

        return parameterType.getDeclaredConstructor(String.class).newInstance(encodedJwt);
    }
}
