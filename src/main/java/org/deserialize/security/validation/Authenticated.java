package com.enelx.bfw.framework.security.validation;

import com.enelx.bfw.framework.security.validation.impl.UniqueIdAuthValidation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Authenticated {
    Class<? extends AbstractAuthValidation> validationClass() default UniqueIdAuthValidation.class;
    String unauthorizedMessage() default "";
}
