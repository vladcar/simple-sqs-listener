package com.vladc.sqslistener.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.intellij.lang.annotations.Language;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SqsMessageHandler {

  /**
   * AWS SQS queue name. Used for retrieving the url of the queue
   */
  String queueName();

  /**
   * Maximum number of messages to return from single receiveMessage call. Valid values: 1 to 10.
   *
   * @see ReceiveMessageRequest#maxNumberOfMessages()
   */
  int maxBatchSize() default 10;

  /**
   * The duration (in seconds) that the received messages are hidden from subsequent retrieve
   * requests after being retrieved by a ReceiveMessage request.
   *
   * @see ReceiveMessageRequest#visibilityTimeout()
   */
  int visibilityTimeout() default 60;

  /**
   * Number of threads polling from this queue.
   */
  int concurrency() default 1;

  /**
   * Enables long polling mode by setting receive wait time to 20 seconds or 1 second if not
   * enabled. Enabled by default.
   *
   * @see ReceiveMessageRequest#waitTimeSeconds()
   */
  boolean longPolling() default true;

  AckMode ackMode() default AckMode.AUTO;

  /**
   * {@linkplain ThreadPoolTaskExecutor} bean that will be used for executing SqsMessageHandler
   * annotated methods. Use SpEL expression e.g {@code @SqsMessageHandler(queueName = "queue",
   * executor = "#{@sqsListenerExec}")}.
   * Note that bean must be {@linkplain ThreadPoolTaskExecutor} type
   */
  @Language("SpEL")
  String executor() default "";

  enum AckMode {

    /**
     * Automatically calls {@linkplain SqsClient#deleteMessageBatch} if annotated method returns
     * successfully.
     */
    AUTO,

    /**
     * Messages are not removed from the queue automatically. Using this mode means manually
     * deleting messages from the queue.
     */
    MANUAL
  }
}
