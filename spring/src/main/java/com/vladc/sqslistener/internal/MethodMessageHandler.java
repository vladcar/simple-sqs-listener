package com.vladc.sqslistener.internal;

import com.vladc.sqslistener.MessageHandler;
import java.lang.reflect.Method;
import org.springframework.util.ReflectionUtils;
import software.amazon.awssdk.services.sqs.model.Message;

class MethodMessageHandler implements MessageHandler {

  private final Method method;
  private final Object target;

  MethodMessageHandler(Method method, Object target) {
    this.method = method;
    this.target = target;
  }

  @Override
  public void handle(Message message) {
    ReflectionUtils.makeAccessible(method);
    ReflectionUtils.invokeMethod(method, target, message);
  }
}
