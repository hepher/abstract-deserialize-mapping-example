package com.enel.eic.commons.aspect;


import com.enel.eic.commons.model.entity.TracedRequest;
import com.enel.eic.commons.property.ApplicationProperties;
import com.enel.eic.commons.service.TracedRequestService;
import com.enel.eic.commons.util.LabelUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;

@Slf4j
@Aspect
@Component
@ConditionalOnExpression("${commons.aspect.controller.enabled:true}")
public class ControllerLoggingAspect extends AbstractLoggingAspect {

    private final static ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private TracedRequestService tracedRequestService;

    @Value("${commons.aspect.controller.traced-headers:null}")
    private List<String> trackedHeaderList;

    @Value("${commons.aspect.controller.success-operation-trace.enable:false}")
    private boolean successOperationTraceEnabled;

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void trackingControllerExecution() {}

    @Around("trackingControllerExecution() && trackingPackagePointcut()")
    public Object aroundControllerExecution(ProceedingJoinPoint joinPoint) throws Throwable {

        LoggingAspectParameter parameter = new LoggingAspectParameter(joinPoint, "Controller");

        if (trackedHeaderList == null) {
            trackedHeaderList = new ArrayList<>();
        }

        trackedHeaderList.addAll(Arrays.asList(LabelUtils.MASHERY_UNIQUE_ID_HEADER, LabelUtils.ANNOTATION_TRANSACTION_ID, LabelUtils.HEADER_AUTHENTICATION));

        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();

        String requestTransactionId = request.getHeader(LabelUtils.TRANSACTION_ID);

        Map<String, String> filtredHeaderMap = new HashMap<>();

        for (String headerName : Collections.list(request.getHeaderNames())) {
            if (trackedHeaderList.contains(headerName)) {
                filtredHeaderMap.put(headerName, request.getHeader(headerName));
            }
        }

        parameter.setDetail(new JoinPointDetail(joinPoint, requestTransactionId));

        TracedRequest tracedRequest = new TracedRequest();
        tracedRequest.setInsertDateTime(new Date());
        tracedRequest.setPath(request.getServletPath());
        tracedRequest.setHttpMethod(request.getMethod());
        tracedRequest.setHeader(filtredHeaderMap);

        parameter.setSuccessConsumer((joinPointDetail, result) -> {
            if (successOperationTraceEnabled) {
                tracedRequest.setTransactionId(joinPointDetail.getTransactionId());
                tracedRequest.setMethod(joinPointDetail.getMethod());

                try {
                    tracedRequest.setRequestBody(mapper.writeValueAsString(joinPointDetail.getSimpleParameterMap()));
                    tracedRequest.setResponseBody(mapper.writeValueAsString(result));
                } catch (JsonProcessingException e) {
//                    throw new RuntimeException(e);
                }

                tracedRequest.setResponseStatus(HttpStatus.OK.value());

                tracedRequestService.save(tracedRequest);
            }
        });

        parameter.setErrorConsumer((joinPointDetail, bfwException) -> {
            tracedRequest.setTransactionId(joinPointDetail.getTransactionId());
            tracedRequest.setMethod(joinPointDetail.getMethod());

            try {
                tracedRequest.setRequestBody(StringUtils.substring(mapper.writeValueAsString(joinPointDetail.getSimpleParameterMap()), 0, 255));
            } catch (JsonProcessingException e) {
//                requestBody = joinPointDetail.getSimpleParameterMap().toString();
            }

            tracedRequest.setResponseBody(bfwException.getSystemErrorResponse() != null ? bfwException.getSystemErrorResponse() : bfwException.getMessage());
            tracedRequest.setResponseStatus(bfwException.getHttpStatus().value());
            tracedRequest.setErrorCode(bfwException.getErrorCode());

            tracedRequestService.save(tracedRequest);
        });

        return proceed(parameter);
    }
}
