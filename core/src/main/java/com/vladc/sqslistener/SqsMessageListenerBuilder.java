package com.vladc.sqslistener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import software.amazon.awssdk.services.sqs.SqsClient;

public class SqsMessageListenerBuilder {

  private SqsClient sqsClient;
  private ExecutorService executor;
  private String url;
  private Integer maxBatchSize = 10;
  private Integer visibilityTimeoutSeconds = 60;
  private Boolean longPolling = true;
  private MessageHandler messageHandler;
  private ErrorHandler errorHandler;
  private Boolean autoAcknowledge = true;
  private Integer concurrency = 1;
  private List<HandlerInterceptor> interceptors = new ArrayList<>();

  public SqsMessageListenerBuilder client(SqsClient client) {
    this.sqsClient = client;
    return this;
  }

  public SqsMessageListenerBuilder executor(ExecutorService executor) {
    this.executor = executor;
    return this;
  }

  public SqsMessageListenerBuilder url(String url) {
    this.url = url;
    return this;
  }

  public SqsMessageListenerBuilder maxBatchSize(Integer maxBatchSize) {
    this.maxBatchSize = maxBatchSize;
    return this;
  }

  public SqsMessageListenerBuilder visibilityTimeoutSeconds(Integer visibilityTimeoutSeconds) {
    this.visibilityTimeoutSeconds = visibilityTimeoutSeconds;
    return this;
  }

  public SqsMessageListenerBuilder longPolling(Boolean longPolling) {
    this.longPolling = longPolling;
    return this;
  }

  public SqsMessageListenerBuilder handler(MessageHandler messageHandler) {
    this.messageHandler = messageHandler;
    return this;
  }

  public SqsMessageListenerBuilder errorHandler(ErrorHandler errorHandler) {
    this.errorHandler = errorHandler;
    return this;
  }

  public SqsMessageListenerBuilder autoAcknowledge(Boolean autoAcknowledge) {
    this.autoAcknowledge = autoAcknowledge;
    return this;
  }

  public SqsMessageListenerBuilder concurrency(Integer concurrency) {
    this.concurrency = concurrency;
    return this;
  }

  public SqsMessageListenerBuilder interceptors(List<HandlerInterceptor> interceptors) {
    this.interceptors = interceptors;
    return this;
  }

  public SqsMessageListener build() {
    SqsQueue queue =
        new SqsQueue(
            this.url,
            this.maxBatchSize,
            this.visibilityTimeoutSeconds,
            this.messageHandler,
            this.errorHandler,
            this.longPolling,
            this.autoAcknowledge,
            this.concurrency,
            this.interceptors);

    return new SqsMessageListener(this.sqsClient, this.executor, queue);
  }
}
