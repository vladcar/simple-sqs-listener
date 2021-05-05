package com.vladc.sqslistener.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SqsMessageListenerHandler {

  /**
   * AWS SQS queue name. Used for retrieving the url of the queue
   *
   * @return queue name
   */
  String queueName();

  /**
   * Maximum number of messages to return from single receiveMessage call. Valid values: 1 to 10.
   * This value is used for building {@linkplain software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest}
   */
  int maxBatchSize() default 10;

  /**
   * The duration (in seconds) that the received messages are hidden from subsequent retrieve
   * requests after being retrieved by a ReceiveMessage request. This value is used for building
   * {@linkplain ReceiveMessageRequest}
   */
  int visibilityTimeout() default 60;

  /**
   * Number of threads polling from this queue.
   *
   * @apiNote Consider adjusting {@linkplain SqsMessageListenerHandler#messageProcessorPoolSize()}
   * based on number of concurrent consumers and maximum batch size.
   */
  int concurrentConsumers() default 1;

  /**
   * Size of the pool used for invoking {@linkplain SqsMessageListenerHandler} annotated methods
   *
   * @apiNote Consider adjusting this value based on number of concurrent consumers and maximum
   * batch size.
   */
  int messageProcessorPoolSize() default 1;

  /**
   * Enables long polling. If true, sets {@linkplain ReceiveMessageRequest#waitTimeSeconds()} to 20
   * seconds, 1 second otherwise
   */
  boolean longPolling() default true;

  /**
   * If true, automatically calls {@linkplain software.amazon.awssdk.services.sqs.SqsClient#deleteMessageBatch}
   * if handler method returns successfully
   */
  boolean autoAcknowledge() default true;
}
