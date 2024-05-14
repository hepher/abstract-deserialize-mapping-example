package com.enelx.bfw.framework.security.validation;

import com.enelx.bfw.framework.exception.BfwException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthRequestInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object object) throws Exception {
        if (!(object instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        if (!handlerMethod.hasMethodAnnotation(Authenticated.class)) {
            return true;
        }

        Authenticated methodAnnotation = handlerMethod.getMethodAnnotation(Authenticated.class);
        if (methodAnnotation == null) {
            return true;
        }

        AbstractAuthValidation authAnnotationClass = methodAnnotation.validationClass().getDeclaredConstructor().newInstance();
        if (!authAnnotationClass.authenticate(request, handlerMethod)) {
            throw new BfwException(StringUtils.defaultIfBlank(methodAnnotation.unauthorizedMessage(), authAnnotationClass.unauthorizedMessage()), String.valueOf(HttpStatus.UNAUTHORIZED.value()), HttpStatus.UNAUTHORIZED, null);
        }

        return true;
    }
//    @Override
//    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object object, ModelAndView model){
//        System.out.println("INTERCEPTOR - POST HANDLER");
//    }
//
//    @Override
//    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object object, Exception exception){
//        System.out.println("INTERCEPTOR - AFTER COMPLETION");
//    }
}
