package com.vladc.sqslistener.annotation;

import com.vladc.sqslistener.ErrorHandler;
import com.vladc.sqslistener.SqsMessageListener;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

/**
 * Configures new {@linkplain SqsMessageListener} using annotation arguments. Annotated beans will
 * be registered as message handler for created listener. Currently, supported signatures:
 *
 * <ul>
 *   <li>{@link Message} complete SQS message being processed.
 * </ul>
 *
 * Exception handling
 *
 * <ul>
 *   <li>Any exception thrown for this method will be caught and logged by listener
 *   <li>A bean provided in {@linkplain SqsListener#exceptionHandler()} will be called
 * </ul>
 *
 * @see SqsMessageListener
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SqsListener {

  /**
   * AWS SQS queue url.
   *
   * @return the queue url or expression (SpEL)
   */
  String url() default "";

  /**
   * Maximum number of messages to return from single receiveMessage call. Valid values: 1 to 10.
   *
   * @return maximum batch size or expression (SpEL)
   * @see ReceiveMessageRequest#maxNumberOfMessages()
   */
  String maxBatchSize() default "10";

  /**
   * The duration (in seconds) that the received messages are hidden from subsequent retrieve
   * requests after being retrieved by a ReceiveMessage request.
   *
   * @see ReceiveMessageRequest#visibilityTimeout()
   */
  String visibilityTimeout() default "60";

  /** Number of threads polling from this queue. */
  String concurrency() default "1";

  /**
   * {@linkplain ErrorHandler} bean that will be called when @SqsMessageListener method throws an
   * exception
   */
  String exceptionHandler() default "";

  PollMode pollMode() default PollMode.LONG;

  AckMode ackMode() default AckMode.AUTO;

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
    MANUAL;
  }

  enum PollMode {

    /**
     * Wait 20 seconds for the message to become available before making request to SQS. If a
     * message is available, the call returns sooner than 20 seconds. If no messages are available
     * and the wait time expires, the call returns successfully with an empty list of messages. This
     * is recommended and cost efficient way of polling.
     *
     * @see ReceiveMessageRequest#waitTimeSeconds()
     */
    LONG,

    /**
     * Wait up to 1 second for the message to become available before making request to SQS. Is far
     * less cost-efficient comparing to PollMode.LONG - use only for high-volume queues under
     * constant load.
     *
     * @see ReceiveMessageRequest#waitTimeSeconds()
     */
    SHORT;
  }
}
