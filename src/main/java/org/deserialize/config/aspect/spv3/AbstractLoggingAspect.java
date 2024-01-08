package com.enelx.bfw.framework.aspect;

import com.enelx.bfw.framework.exception.BfwException;
import com.enelx.bfw.framework.util.LabelUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.util.StopWatch;

import java.util.function.BiConsumer;

@Slf4j
public abstract class AbstractLoggingAspect {

    protected JoinPointDetail detail;
    protected String klassType = "AbstractLoggingAspect";
    protected String transactionId;
    protected BiConsumer<JoinPointDetail, Object> successConsumer;
    protected BiConsumer<JoinPointDetail, BfwException> errorConsumer;
    protected LoggingKlassStrategy loggingKlassStrategy = LoggingKlassStrategy.KLASS_METHOD;

    public enum LoggingKlassStrategy {
        EXECUTION_METHOD {
            @Override
            public String getKlass(JoinPointDetail joinPointDetail) {
                return joinPointDetail.getExecutionKlass();
            }
        },
        KLASS_METHOD {
            @Override
            public String getKlass(JoinPointDetail joinPointDetail) {
                return joinPointDetail.getMethodKlass();
            }
        };

        public abstract String getKlass(JoinPointDetail joinPointDetail);
    }

    @Pointcut("execution(* com.enelx..*.* (..))")
    protected void trackingPackagePointcut() {}

    protected Object proceed(ProceedingJoinPoint joinPoint) throws Throwable {

        if (detail == null) {
            detail = new JoinPointDetail(joinPoint, transactionId);
        }

        StopWatch watcher = new StopWatch();
        try {
            log.info(LabelUtils.START_LOG_PARAMS + detail.getParameterListAsString(), klassType, loggingKlassStrategy.getKlass(detail), detail.getMethod());
            watcher.start("Starting method execution: " + detail.getMethod());

            MDC.put(LabelUtils.TRANSACTION_ID, detail.getTransactionId());

            Object result = joinPoint.proceed();
            if (successConsumer != null) {
                successConsumer.accept(detail, result);
            }
            return result;
        } catch (BfwException e) {
            log.error(LabelUtils.EXIT_LOG_HEADER_ERROR, klassType, loggingKlassStrategy.getKlass(detail), detail.getMethod(), e.getLocalizedMessage());

            if (errorConsumer != null) {
                errorConsumer.accept(detail, e);
            }
            throw e;
        } catch (Throwable e) {
            log.error(LabelUtils.EXIT_LOG_HEADER_ERROR, klassType, loggingKlassStrategy.getKlass(detail), detail.getMethod(), e.getLocalizedMessage());
            log.error(ExceptionUtils.getStackTrace(e));

            BfwException bfwException = new BfwException(e.getLocalizedMessage(), detail.getTransactionId(), null);
            if (errorConsumer != null) {
                errorConsumer.accept(detail, bfwException);
            }
            throw e;
        } finally {
            watcher.stop();
            for (StopWatch.TaskInfo taskInfo : watcher.getTaskInfo()) {
                log.debug(LabelUtils.TASK_INFO_LOG, klassType, loggingKlassStrategy.getKlass(detail), detail.getMethod(), taskInfo.getTaskName(), taskInfo.getTimeMillis());
            }
            log.info(LabelUtils.EXIT_LOG_HEADER_RESULT, klassType, loggingKlassStrategy.getKlass(detail), detail.getMethod(), watcher);
        }
    }
}
