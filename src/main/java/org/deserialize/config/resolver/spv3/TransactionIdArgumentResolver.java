package com.enelx.bfw.framework.resolver;

import com.enelx.bfw.framework.util.LabelUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

//import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

@Component
public class TransactionIdArgumentResolver extends AbstractArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        return methodParameter.hasParameterAnnotation(TransactionId.class);
    }

    @Override
    public Object resolveArgument(MethodParameter methodParameter, ModelAndViewContainer modelAndViewContainer, NativeWebRequest nativeWebRequest, WebDataBinderFactory webDataBinderFactory) throws Exception {
        HttpServletRequest request = (HttpServletRequest) nativeWebRequest.getNativeRequest();
        TransactionId parameterAnnotation = methodParameter.getParameterAnnotation(TransactionId.class);

        String transactionId = null;
        if (parameterAnnotation != null) {
            transactionId = (String) RequestParameterUtils.getValueFromRequest(request, parameterAnnotation.type(), parameterAnnotation.value());
        }

        if (transactionId == null) {
            transactionId = ApplicationContextUtils.getTransactionId();
        }

        if (transactionId == null) {
            transactionId = UUID.randomUUID().toString();
        }

        return transactionId;
    }
}
