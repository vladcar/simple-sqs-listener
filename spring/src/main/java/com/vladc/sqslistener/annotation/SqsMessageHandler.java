package com.vladc.sqslistener.annotation;

import com.vladc.sqslistener.ErrorHandler;
import com.vladc.sqslistener.SqsConfigurer;
import com.vladc.sqslistener.SqsMessageListener;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.intellij.lang.annotations.Language;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

/**
 * Configures new {@linkplain SqsMessageListener} using annotation arguments. Annotated method will
 * be registered as message handler for created listener. Currently supported signatures:
 * <ul>
 * <li>{@link Message} complete SQS message being processed.</li>
 * </ul>
 * Exception handling
 * <ul>
 *   <li>Any exception thrown for this method will be caught and logged by listener</li>
 *   <li>A bean provided in {@linkplain SqsMessageHandler#exceptionHandler()} will be called</li>
 *   <li>Exception will prevent message from being auto-acknowledged if {@linkplain #ackMode()} is set to AUTO</li>
 * </ul>
 *
 * @see SqsMessageListener
 * @see com.vladc.sqslistener.MessageListenerAnnotatedMethodBeanPostProcessor
 * @see com.vladc.sqslistener.SqsMessageListenerManager
 * @see EnableSqs
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SqsMessageHandler {

  /**
   * AWS SQS queue name. Used for retrieving the url of the queue
   */
  String queueName() default "";

  /**
   * {@linkplain SqsConfigurer} bean. Useful when queue configuration is not constant e.g. queue url
   * comes from environment variable. Attributes specified in SqsConfigurer bean take precedence
   * over annotation attributes. Use SpEL expression e.g {@code @SqsMessageHandler(config =
   * "#{@myQueue}")}.
   */
  @Language("SpEL") String config() default "";

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

  PollMode pollMode() default PollMode.LONG;

  AckMode ackMode() default AckMode.AUTO;

  /**
   * {@linkplain ThreadPoolTaskExecutor} bean that will be used for executing SqsMessageHandler
   * annotated methods. Use SpEL expression e.g {@code @SqsMessageHandler(queueName = "queue",
   * executor = "#{@sqsListenerExec}")}. Note that bean must be {@linkplain ThreadPoolTaskExecutor}
   * type
   */
  @Language("SpEL") String executor() default "";

  /**
   * {@linkplain ErrorHandler} bean that will be called when @SqsMessageHandler method throws an
   * exception
   */
  @Language("SpEL") String exceptionHandler() default "";

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
     * less cost efficient comparing to PollMode.LONG - use only for high-volume queues under
     * constant load.
     *
     * @see ReceiveMessageRequest#waitTimeSeconds()
     */
    SHORT;
  }
}
