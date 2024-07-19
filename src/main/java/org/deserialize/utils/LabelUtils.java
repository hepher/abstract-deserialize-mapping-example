package com.enelx.bfw.framework.util;

import org.apache.commons.lang3.StringUtils;

public interface LabelUtils {

    String AWS_CLOUD_WATCH_LINE_SEPARATOR = "\r";

    String JWT_VALUE = "X-JWT-TOKEN";
    String TRANSACTION_ID = "trace-id";
    String HEADER_AUTHENTICATION = "Authentication";
    String ANNOTATION_TRANSACTION_ID = "trace-id";
    String MASHERY_UNIQUE_ID_HEADER = "X-UniqueID";
    String EXW_UNIQUE_ID_PARAMETER_VALUE = "exw-unique-id";
    String CORRELATION_ID = "correlation-id";
    String VERSION = "version";
    String MODULE_ID = "module-id";
    String NAMESPACE = "namespace";
    String CONTAINER = "container";
    String SPAN_ID = "span-id";

    String START_LOG_PARAMS = "START {}={}, Method={}, Params ::: ";
    String FORMAT_LOG_HEADER = "{}={} Method={} ::: ";
    String TASK_INFO_LOG = FORMAT_LOG_HEADER + "Task: '{}' completed in '{}' ms";
    String EXIT_LOG_HEADER_RESULT = "EXIT {}={}, Method={}, Result={}";
    String EXIT_LOG_HEADER_ERROR = "EXIT {}={}, Method={}, Error={}";
    String EXIT_ERROR_LOG_HEADER_ERROR = "ERROR {}={}, Method={}, Error={}";
    String LOG_REQUEST_RESPONSE = AWS_CLOUD_WATCH_LINE_SEPARATOR + "=============================== REQUEST/RESPONSE ===============================";
    String LOG_REQUEST_BEGIN = "=============================== REQUEST BEGIN    ===============================";
    String LOG_CLIENT_REQUEST_BEGIN = AWS_CLOUD_WATCH_LINE_SEPARATOR + "=============================== CLIENT REQUEST BEGIN    ===============================";
    String LOG_CLIENT_RESPONSE_BEGIN = "=============================== CLIENT RESPONSE BEGIN    ===============================";
    String LOG_REQUEST_END = "=============================== REQUEST END      ===============================";
    String LOG_CLIENT_REQUEST_END = "=============================== CLIENT REQUEST END      ===============================";
    String LOG_CLIENT_RESPONSE_END = "=============================== CLIENT RESPONSE END      ===============================";
    String LOG_RESPONSE_BEGIN = "=============================== RESPONSE BEGIN   ===============================";
    String LOG_RESPONSE_END = "=============================== RESPONSE END     ===============================";

    String LOG_INTERNAL_METHOD = "EXECUTION Class={}, Method={} ::: ";

    String LOG_URI = "URI          : {}";
    String LOG_METHOD = "Method       : {}";
    String LOG_HEADERS = "Headers      : {}";
    String LOG_DURATION = "Duration     : {}";
    String LOG_BODY = "Body         : {}";
    String LOG_STATUS_CODE = "Status code  : {}";
    String LOG_CLIENT_REQUEST = StringUtils.join(new String[] { LOG_CLIENT_REQUEST_BEGIN, LOG_URI, LOG_METHOD, LOG_HEADERS, LOG_BODY, LOG_CLIENT_REQUEST_END }, AWS_CLOUD_WATCH_LINE_SEPARATOR);
    String LOG_CLIENT_REQUEST_RESPONSE = StringUtils.join(new String[] { LOG_CLIENT_REQUEST_BEGIN, LOG_URI, LOG_METHOD, LOG_HEADERS, LOG_DURATION, LOG_BODY, LOG_CLIENT_REQUEST_END, LOG_CLIENT_RESPONSE_BEGIN, LOG_STATUS_CODE, LOG_HEADERS, LOG_BODY, LOG_CLIENT_RESPONSE_END }, AWS_CLOUD_WATCH_LINE_SEPARATOR);
    String LOG_REST_CALL_REQUEST_RESPONSE = StringUtils.join(new String[] { LOG_REQUEST_RESPONSE, LOG_REQUEST_BEGIN, LOG_URI, LOG_METHOD, LOG_HEADERS, LOG_DURATION, LOG_BODY, LOG_REQUEST_END, LOG_RESPONSE_BEGIN, LOG_STATUS_CODE, LOG_HEADERS, LOG_BODY, LOG_RESPONSE_END }, AWS_CLOUD_WATCH_LINE_SEPARATOR);
}
