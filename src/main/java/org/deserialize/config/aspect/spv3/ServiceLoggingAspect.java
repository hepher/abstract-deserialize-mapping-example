package com.enelx.bfw.framework.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Aspect
@Component
@ConditionalOnExpression("${aspect.service.enabled:true}")
public class ServiceLoggingAspect extends AbstractLoggingAspect {

    @Pointcut("execution(* com.enelx..service..*.*(..))")
    public void trackingServiceExecution() {}

    @Around("trackingServiceExecution() && trackingPackagePointcut()")
    public Object aroundServiceExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        return proceed(joinPoint, "Service");
    }
}
