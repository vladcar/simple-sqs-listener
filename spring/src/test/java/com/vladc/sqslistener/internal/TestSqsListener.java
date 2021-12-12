package com.vladc.sqslistener.internal;

import com.vladc.sqslistener.annotation.SqsHandler;
import com.vladc.sqslistener.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.model.Message;

@Slf4j
@SqsListener(
    url = "${messaging.sqs.queueUrl}",
    concurrency = "${messaging.sqs.concurrency}",
    maxBatchSize = "${messaging.sqs.maxBatchSize}",
    visibilityTimeout = "${messaging.sqs.visibilityTimeoutSeconds}",
    exceptionHandler = "#{@testErrorHandler}")
public class TestSqsListener {

  @SqsHandler(messageType = "test-message-type")
  void downloadProfile(Message message) {}

  @SqsHandler(isDefault = true)
  void fallback(Message message) {
    log.warn("fallback method called");
  }
}
