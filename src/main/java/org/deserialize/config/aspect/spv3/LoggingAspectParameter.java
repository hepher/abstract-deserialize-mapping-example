package com.enelx.bfw.framework.aspect;

import com.enelx.bfw.framework.exception.BfwException;
import lombok.Data;
import org.aspectj.lang.ProceedingJoinPoint;

import java.util.function.BiConsumer;


@Data
public class LoggingAspectParameter {

    private ProceedingJoinPoint joinPoint;
    private JoinPointDetail detail;
    private String klassType;
    private String transactionId;
    private BiConsumer<JoinPointDetail, Object> successConsumer;
    private BiConsumer<JoinPointDetail, BfwException> errorConsumer;
    private AbstractLoggingAspect.LoggingKlassStrategy loggingKlassStrategy = AbstractLoggingAspect.LoggingKlassStrategy.KLASS_METHOD;

    public LoggingAspectParameter(ProceedingJoinPoint joinPoint, String klassType) {
        this.joinPoint = joinPoint;
        this.klassType = klassType;
    }
}
