package com.vladc.sqslistener;

import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@RequiredArgsConstructor
@Getter
class SqsQueue {

  /** AWS SQS queue url */
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
  private final MessageHandler handler;

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

  private final Integer concurrency;

  private final List<HandlerInterceptor> interceptors;

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
}
