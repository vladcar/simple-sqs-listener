package com.vladc.sqslistener.internal;

import com.vladc.sqslistener.MessageHandler;
import java.lang.reflect.Method;
import java.util.Map;
import org.springframework.util.ReflectionUtils;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

class MessageAttributeMappingMessageHandler implements MessageHandler {

  private static final String WELL_KNOWN_ATTRIBUTE_KEY = "messageType";

  private final Object target;
  private final MethodMapping methodMapping;

  MessageAttributeMappingMessageHandler(Object target, MethodMapping methodMapping) {
    this.target = target;
    this.methodMapping = methodMapping;
  }

  @Override
  public void handle(Message message) {
    Map<String, MessageAttributeValue> attributeValueMap = message.messageAttributes();
    MessageAttributeValue attributeValue = attributeValueMap.get(WELL_KNOWN_ATTRIBUTE_KEY);
    if (attributeValue == null) {
      Method defaultMethod = methodMapping.getDefaultMethod();
      if (defaultMethod != null) {
        invoke(defaultMethod, message);
        return;
      }
      throw new IllegalStateException("unable to handle sqs message - method mapping not found");
    }

    Method handlerMethod = methodMapping.getMethod(attributeValue.stringValue());
    if (handlerMethod != null) {
      invoke(handlerMethod, message);
    } else {
      throw new IllegalStateException(
          "unable to handle sqs message - method mapping not found for %s"
              .formatted(attributeValue.stringValue()));
    }
  }

  private void invoke(Method method, Message message) {
    ReflectionUtils.makeAccessible(method);
    ReflectionUtils.invokeMethod(method, target, message);
  }

  static class MethodMapping {
    private final Map<String, Method> attributeMethodMap;
    private final Method defaultMethod;

    public MethodMapping(Map<String, Method> attributeMethodMap, Method defaultMethod) {
      this.attributeMethodMap = attributeMethodMap;
      this.defaultMethod = defaultMethod;
    }

    public Method getMethod(String attributeValue) {
      return attributeMethodMap.getOrDefault(attributeValue, this.defaultMethod);
    }

    public Method getDefaultMethod() {
      return this.defaultMethod;
    }
  }
}
