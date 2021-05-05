package com.vladc.sqslistener.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SqsMessageListenerHandler {

  String queue();

  int maxBatchSize() default 10;

  int visibilityTimeoutSeconds() default 60;

  int concurrentConsumers() default 1;

  int messageProcessorPoolSize() default 1;

  String errorHandler() default "";

  boolean longPolling() default true;

  boolean autoAcknowledge() default true;
}
