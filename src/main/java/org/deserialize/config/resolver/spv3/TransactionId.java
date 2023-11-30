package com.enelx.bfw.framework.resolver;

import com.enelx.bfw.framework.util.LabelUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface TransactionId {
    String value() default LabelUtils.ANNOTATION_TRANSACTION_ID;
    String description() default "EXW transaction ID";
    boolean required() default false;
    ParameterType type() default ParameterType.HEADER;
}
