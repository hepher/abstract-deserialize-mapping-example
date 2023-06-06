package org.deserialize.config.resolver;

import springfox.documentation.service.ParameterType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface CustomValue {
    String value() default "X-CustomValue";
    String name() default "X-CustomValue";
    String description() default "X-CustomValue";
    boolean required() default false;
    ParameterType type() default ParameterType.HEADER;
}
