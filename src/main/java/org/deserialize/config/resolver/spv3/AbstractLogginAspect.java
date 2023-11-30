package com.enelx.bfw.framework.aspect;

import com.enelx.bfw.framework.exception.BfwException;
import com.enelx.bfw.framework.util.LabelUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
public abstract class AbstractLoggingAspect {

    @Pointcut("execution(* com.enelx..*.* (..))")
    protected void trackingPackagePointcut() {}

    protected Object proceed(ProceedingJoinPoint joinPoint, String classType) throws Throwable {
        return proceed(joinPoint, classType, null, null);
    }

    protected Object proceed(ProceedingJoinPoint joinPoint, String classType, BiConsumer<JoinPointDetail, Object> successConsumer, BiConsumer<JoinPointDetail, BfwException> errorConsumer) throws Throwable {

        JoinPointDetail detail = new JoinPointDetail(joinPoint);

        StopWatch watcher = new StopWatch();
        try {
            log.info(LabelUtils.START_LOG_PARAMS + detail.getParameterListAsString(), classType, detail.getKlass(), detail.getMethod());
            watcher.start("Starting method execution: " + detail.getMethod());

            MDC.put(LabelUtils.TRANSACTION_ID, detail.getTransactionId());

            Object result = joinPoint.proceed();
            if (successConsumer != null) {
                successConsumer.accept(detail, result);
            }
            return result;
        } catch (BfwException e) {
            log.error(LabelUtils.EXIT_LOG_HEADER_ERROR, classType, detail.getKlass(), detail.getMethod(), e.getLocalizedMessage());

            if (errorConsumer != null) {
                errorConsumer.accept(detail, e);
            }
            throw e;
        } catch (Throwable e) {
            log.error(LabelUtils.EXIT_LOG_HEADER_ERROR, classType, detail.getKlass(), detail.getMethod(), e.getLocalizedMessage());
            log.error(ExceptionUtils.getStackTrace(e));

            BfwException bfwException = new BfwException(e.getLocalizedMessage(), detail.getTransactionId(), null);
            if (errorConsumer != null) {
                errorConsumer.accept(detail, bfwException);
            }
            throw e;
        } finally {
            watcher.stop();
            for (StopWatch.TaskInfo taskInfo : watcher.getTaskInfo()) {
                log.debug(LabelUtils.TASK_INFO_LOG, classType, detail.getKlass(), detail.getMethod(), taskInfo.getTaskName(), taskInfo.getTimeMillis());
            }
            log.info(LabelUtils.EXIT_LOG_HEADER_RESULT, classType, detail.getKlass(), detail.getMethod(), watcher);
        }
    }
}
