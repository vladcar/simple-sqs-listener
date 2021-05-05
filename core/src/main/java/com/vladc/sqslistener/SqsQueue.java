package com.vladc.sqslistener;

import java.util.Objects;

public class SqsQueue {

  private final String url;
  private final int maxBatchSize;
  private final int visibilityTimeoutSeconds;
  private final MessageHandler messageHandler;
  private final ErrorHandler errorHandler;
  private final Boolean longPolling;
  private final Boolean autoAcknowledge;

  private SqsQueue(String url, int maxBatchSize, int visibilityTimeoutSeconds,
      Boolean longPolling, MessageHandler messageHandler,
      ErrorHandler errorHandler, Boolean autoAcknowledge) {
    Objects.requireNonNull(messageHandler, "MessageHandler must be non-null");
    Objects.requireNonNull(url, "Queue url must be non-null");

    this.url = url;
    this.maxBatchSize = maxBatchSize;
    this.visibilityTimeoutSeconds = visibilityTimeoutSeconds;
    this.longPolling = longPolling;
    this.messageHandler = messageHandler;
    this.errorHandler = errorHandler;
    this.autoAcknowledge = Objects.requireNonNullElse(autoAcknowledge, Boolean.TRUE);
  }

  public String getUrl() {
    return url;
  }

  public int getMaxBatchSize() {
    return maxBatchSize;
  }

  public int getVisibilityTimeoutSeconds() {
    return visibilityTimeoutSeconds;
  }

  public Boolean isLongPolling() {
    return longPolling;
  }

  public MessageHandler getMessageHandler() {
    return messageHandler;
  }

  public ErrorHandler getErrorHandler() {
    return errorHandler;
  }

  public Boolean isAutoAcknowledge() {
    return autoAcknowledge;
  }

  public static SqsQueueBuilder builder() {
    return new SqsQueueBuilder();
  }

  public static class SqsQueueBuilder {

    private String url;
    private int maxBatchSize;
    private int visibilityTimeoutSeconds;
    private Boolean longPolling;
    private MessageHandler messageHandler;
    private ErrorHandler errorHandler;
    private Boolean autoAcknowledge;

    public SqsQueueBuilder url(String url) {
      this.url = url;
      return this;
    }

    public SqsQueueBuilder maxBatchSize(int maxBatchSize) {
      this.maxBatchSize = maxBatchSize;
      return this;
    }

    public SqsQueueBuilder visibilityTimeoutSeconds(int visibilityTimeoutSeconds) {
      this.visibilityTimeoutSeconds = visibilityTimeoutSeconds;
      return this;
    }

    public SqsQueueBuilder longPolling(Boolean longPolling) {
      this.longPolling = longPolling;
      return this;
    }

    public SqsQueueBuilder messageHandler(MessageHandler messageHandler) {
      this.messageHandler = messageHandler;
      return this;
    }

    public SqsQueueBuilder errorHandler(ErrorHandler errorHandler) {
      this.errorHandler = errorHandler;
      return this;
    }

    public SqsQueueBuilder autoAcknowledge(Boolean autoAcknowledge) {
      this.autoAcknowledge = autoAcknowledge;
      return this;
    }

    public SqsQueue build() {
      return new SqsQueue(this.url, this.maxBatchSize, this.visibilityTimeoutSeconds,
          this.longPolling, this.messageHandler, this.errorHandler, this.autoAcknowledge);
    }
  }
}
