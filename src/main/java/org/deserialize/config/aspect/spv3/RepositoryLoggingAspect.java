package com.enelx.bfw.framework.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Aspect
@Component
@ConditionalOnExpression("${aspect.repository.enabled:true}")
public class RepositoryLoggingAspect extends AbstractLoggingAspect {

    @Pointcut("within(org.springframework.data.mongodb.repository.MongoRepository+)")
    public void trackingRepositoryExecution() {}

    @Around("trackingRepositoryExecution() && trackingPackagePointcut()")
    public Object aroundControllerExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        return proceed(new LoggingAspectParameter(joinPoint, "Repository"));
    }
}
