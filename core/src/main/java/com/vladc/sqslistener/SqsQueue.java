package com.vladc.sqslistener;

import java.util.Objects;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

public class SqsQueue {

  /**
   * AWS SQS queue url
   */
  private final String url;

  /**
   * Maximum number of messages to poll in a single request
   *
   * @see ReceiveMessageRequest#maxNumberOfMessages()
   */
  private final Integer maxBatchSize;

  /**
   * The duration (in seconds) that the received messages are hidden from subsequent retrieve
   * requests
   *
   * @see ReceiveMessageRequest#visibilityTimeout()
   */
  private final Integer visibilityTimeoutSeconds;

  /**
   * The handler invoked by {@linkplain SqsMessageListener} on each received {@linkplain Message}
   */
  private final MessageHandler messageHandler;

  /**
   * The handler invoked by {@linkplain SqsMessageListener} if exception is thrown from {@linkplain
   * MessageHandler#handle(Message)} method
   */
  private final ErrorHandler errorHandler;

  /**
   * Enables long polling mode by setting receive wait time to 20 seconds or 1 second if not
   * enabled. Enabled by default.
   *
   * @see ReceiveMessageRequest#waitTimeSeconds()
   */
  private final Boolean longPolling;

  /**
   * Enables automatic deletion of message from the queue if handler method returns successfully.
   * Note that disabling autoAcknowledge will prevent messages from being removed form the queue
   * automatically thus must be removed manually by calling {@linkplain
   * SqsClient#deleteMessage(DeleteMessageRequest)}
   *
   * @see SqsClient#deleteMessageBatch
   * @see MessageHandler
   */
  private final Boolean autoAcknowledge;

  private SqsQueue(String url, Integer maxBatchSize, Integer visibilityTimeoutSeconds,
      Boolean longPolling, MessageHandler messageHandler,
      ErrorHandler errorHandler, Boolean autoAcknowledge) {

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

  public Integer getMaxBatchSize() {
    return maxBatchSize;
  }

  public Integer getVisibilityTimeoutSeconds() {
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

  public SqsQueueBuilder toBuilder() {
    return new SqsQueueBuilder()
        .url(this.url)
        .maxBatchSize(this.maxBatchSize)
        .visibilityTimeoutSeconds(this.visibilityTimeoutSeconds)
        .longPolling(this.longPolling)
        .messageHandler(this.messageHandler)
        .errorHandler(this.errorHandler)
        .autoAcknowledge(this.autoAcknowledge);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SqsQueue sqsQueue = (SqsQueue) o;
    return url.equals(sqsQueue.url);
  }

  @Override
  public int hashCode() {
    return Objects.hash(url);
  }

  public static class SqsQueueBuilder {

    private String url;
    private Integer maxBatchSize;
    private Integer visibilityTimeoutSeconds;
    private Boolean longPolling;
    private MessageHandler messageHandler;
    private ErrorHandler errorHandler;
    private Boolean autoAcknowledge;

    public SqsQueueBuilder url(String url) {
      this.url = url;
      return this;
    }

    public SqsQueueBuilder maxBatchSize(Integer maxBatchSize) {
      this.maxBatchSize = maxBatchSize;
      return this;
    }

    public SqsQueueBuilder visibilityTimeoutSeconds(Integer visibilityTimeoutSeconds) {
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
