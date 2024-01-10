package com.enelx.bfw.framework.aspect;

import com.enelx.bfw.framework.exception.BfwException;
import com.enelx.bfw.framework.util.LabelUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.util.StopWatch;

@Slf4j
public abstract class AbstractLoggingAspect {

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

    protected Object proceed(LoggingAspectParameter parameter) throws Throwable {

        JoinPointDetail detail = parameter.getDetail();

        if (detail == null) {
            detail = new JoinPointDetail(parameter.getJoinPoint(), parameter.getTransactionId());
        }

        StopWatch watcher = new StopWatch();
        try {
            log.info(LabelUtils.START_LOG_PARAMS + detail.getParameterListAsString(), parameter.getKlassType(), parameter.getLoggingKlassStrategy().getKlass(detail), detail.getMethod());
            watcher.start("Starting method execution: " + detail.getMethod());

            Object result = parameter.getJoinPoint().proceed();
            if (parameter.getSuccessConsumer() != null) {
                parameter.getSuccessConsumer().accept(detail, result);
            }
            return result;
        } catch (BfwException e) {
            log.error(LabelUtils.EXIT_ERROR_LOG_HEADER_ERROR,  parameter.getKlassType(), parameter.getLoggingKlassStrategy().getKlass(detail), detail.getMethod(), e.getLocalizedMessage());

            if (parameter.getErrorConsumer() != null) {
                parameter.getErrorConsumer().accept(detail, e);
            }
            throw e;
        } catch (Throwable e) {
            log.error(LabelUtils.EXIT_ERROR_LOG_HEADER_ERROR, parameter.getKlassType(), parameter.getLoggingKlassStrategy().getKlass(detail), detail.getMethod(), e.getLocalizedMessage());
            log.error(ExceptionUtils.getStackTrace(e));

            BfwException bfwException = new BfwException(e.getLocalizedMessage(), detail.getTransactionId());
            if (parameter.getErrorConsumer() != null) {
                parameter.getErrorConsumer().accept(detail, bfwException);
            }
            throw e;
        } finally {
            watcher.stop();
            for (StopWatch.TaskInfo taskInfo : watcher.getTaskInfo()) {
                log.debug(LabelUtils.TASK_INFO_LOG, parameter.getKlassType(), parameter.getLoggingKlassStrategy().getKlass(detail), detail.getMethod(), taskInfo.getTaskName(), taskInfo.getTimeMillis());
            }
            log.info(LabelUtils.EXIT_LOG_HEADER_RESULT, parameter.getKlassType(), parameter.getLoggingKlassStrategy().getKlass(detail), detail.getMethod(), watcher);
        }
    }
}
