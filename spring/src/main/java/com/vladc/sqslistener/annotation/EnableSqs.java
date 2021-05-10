package com.vladc.sqslistener.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables autoconfiguration and {@linkplain SqsMessageHandler} method discovery
 *
 * @see com.vladc.sqslistener.autoconfigure.AnnotationSqsMessageListenerAutoConfiguration
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnableSqs {

}
