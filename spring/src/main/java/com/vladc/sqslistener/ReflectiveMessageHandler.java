package com.vladc.sqslistener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.springframework.util.ReflectionUtils;
import software.amazon.awssdk.services.sqs.model.Message;

class ReflectiveMessageHandler implements MessageHandler {

  private final Method method;
  private final Object target;

  ReflectiveMessageHandler(Method method, Object target) {
    this.method = method;
    this.target = target;
  }

  @Override
  public void handle(Message message) {
    try {
      ReflectionUtils.makeAccessible(method);
      method.invoke(target, message);
    } catch (InvocationTargetException e) {
      ReflectionUtils.rethrowRuntimeException(e.getTargetException());
    } catch (IllegalAccessException e) {
      ReflectionUtils.rethrowRuntimeException(e);
    }
  }

  @Override
  public String toString() {
    return String.format("%s.%s", method.getDeclaringClass().getName(), method.getName());
  }
}
