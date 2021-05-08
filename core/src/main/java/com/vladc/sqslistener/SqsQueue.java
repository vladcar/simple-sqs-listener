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
  private final int maxBatchSize;

  /**
   * The duration (in seconds) that the received messages are hidden from subsequent retrieve
   * requests
   *
   * @see ReceiveMessageRequest#visibilityTimeout()
   */
  private final int visibilityTimeoutSeconds;

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
