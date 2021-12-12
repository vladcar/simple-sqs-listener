package com.vladc.sqslistener.internal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vladc.sqslistener.internal.MessageAttributeMappingMessageHandler.MethodMapping;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

class MessageAttributeMappingMessageHandlerTest {

  private MessageAttributeMappingMessageHandler underTest;

  @Test
  void handle() throws NoSuchMethodException {
    String mappingAttributeValue = "testAttr";

    TestTarget target = new TestTarget();
    Method method = target.getClass().getMethod("targetMethod", Message.class);
    MethodMapping methodMapping = new MethodMapping(Map.of(mappingAttributeValue, method), method);
    underTest = new MessageAttributeMappingMessageHandler(target, methodMapping);

    Message testMessage = createSqsMessageWithTypeAttribute(mappingAttributeValue);
    assertDoesNotThrow(() -> underTest.handle(testMessage));
  }

  @Test
  void handleMappingNotFoundDefaultMethod() throws NoSuchMethodException {
    String mappingAttributeValue = "testAttr";
    String notExistingAttribute = "notExists";

    TestTarget target = new TestTarget();
    Method method = target.getClass().getMethod("targetMethod", Message.class);
    MethodMapping methodMapping = new MethodMapping(Map.of(mappingAttributeValue, method), method);
    underTest = new MessageAttributeMappingMessageHandler(target, methodMapping);

    Message testMessage = createSqsMessageWithTypeAttribute(notExistingAttribute);

    // should fall back to default method
    assertDoesNotThrow(() -> underTest.handle(testMessage));
  }

  @Test
  void handleMappingNotFoundMissingAttributeDefaultMethod() throws NoSuchMethodException {
    String mappingAttributeValue = "testAttr";

    TestTarget target = new TestTarget();
    Method method = target.getClass().getMethod("targetMethod", Message.class);
    MethodMapping methodMapping = new MethodMapping(Map.of(mappingAttributeValue, method), method);
    underTest = new MessageAttributeMappingMessageHandler(target, methodMapping);
    Message testMessage = createSqsMessage();

    // should fall back to default method
    assertDoesNotThrow(() -> underTest.handle(testMessage));
  }

  @Test
  void handleMappingNotFoundMissingAttribute() {
    TestTarget target = new TestTarget();
    MethodMapping methodMapping = new MethodMapping(Map.of(), null);
    underTest = new MessageAttributeMappingMessageHandler(target, methodMapping);
    Message testMessage = createSqsMessage();

    // should fall back to default method
    assertThatThrownBy(() -> underTest.handle(testMessage))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("unable to handle sqs message - method mapping not found");
  }

  @Test
  void handleMappingNotFound() throws NoSuchMethodException {
    String mappingAttributeValue = "testAttr";
    String notExistingAttribute = "notExists";

    TestTarget target = new TestTarget();
    Method method = target.getClass().getMethod("targetMethod", Message.class);
    MethodMapping methodMapping = new MethodMapping(Map.of(mappingAttributeValue, method), null);
    underTest = new MessageAttributeMappingMessageHandler(target, methodMapping);

    Message testMessage = createSqsMessageWithTypeAttribute(notExistingAttribute);
    assertThatThrownBy(() -> underTest.handle(testMessage))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(
            "unable to handle sqs message - method mapping not found for %s"
                .formatted(notExistingAttribute));
  }

  static class TestTarget {
    public void targetMethod(Message message) {}
  }

  public static Message createSqsMessageWithTypeAttribute(String messageType) {
    return createSqsMessage(
        Map.of("messageType", MessageAttributeValue.builder().stringValue(messageType).build()),
        "");
  }

  public static Message createSqsMessage() {
    return createSqsMessage(Map.of(), "");
  }

  public static Message createSqsMessage(
      Map<String, MessageAttributeValue> messageAttributes, String body) {
    return Message.builder()
        .messageAttributes(messageAttributes)
        .body(body)
        .messageId(UUID.randomUUID().toString())
        .receiptHandle(UUID.randomUUID().toString())
        .build();
  }
}
