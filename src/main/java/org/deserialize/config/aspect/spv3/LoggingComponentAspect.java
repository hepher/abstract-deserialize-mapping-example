package com.enelx.bfw.framework.aspect;

import com.enelx.bfw.framework.util.LabelUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Aspect
@Component
public class LoggingComponentAspect {

    @Pointcut("execution(* com.enelx.bfw.framework.main.LoggingComponent+.log(String, Object...))")
    public void trackingLoggingComponentExecution() {}

    @Around("trackingLoggingComponentExecution()")
    public Object aroundServiceExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        JoinPointDetail joinPointDetail = new JoinPointDetail(joinPoint, null);

        String format = LabelUtils.LOG_INTERNAL_METHOD;

        List<Object> formatArgs = new ArrayList<>();
        formatArgs.add(StringUtils.defaultIfBlank(joinPointDetail.getParentKlass(), joinPointDetail.getExecutionKlass()));
        formatArgs.add(StringUtils.defaultIfBlank(joinPointDetail.getParentMethod(), joinPointDetail.getMethod()));

        if (joinPointDetail.getParameterMap().get("format").value() != null) {
            format += (String) joinPointDetail.getParameterMap().get("format").value();
        }

        if (joinPointDetail.getParameterMap().get("args").value() != null) {
            formatArgs.addAll(Arrays.asList(((Object[]) joinPointDetail.getParameterMap().get("args").value())));
        }

        return joinPoint.proceed(new Object[] {
                format, formatArgs.toArray()
        });
    }
}
