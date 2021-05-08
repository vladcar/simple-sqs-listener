package com.vladc.sqslistener;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

public class SqsMessageListener {

  private static final Logger log = LoggerFactory.getLogger(SqsMessageListener.class);
  private static final String CONSUMER_LOOP_THREAD_PREFIX = "sqs-consumer-loop";
  private static final String MESSAGE_PROCESSOR_THREAD_PREFIX = "sqs-listener";

  private final SqsClient sqsClient;

  private SqsQueue queue;
  private ExecutorService taskExecutor;
  private ExecutorService consumerLoopExecutor;
  private final Map<String, Future<?>> consumerMap = new ConcurrentHashMap<>();

  private int consumerCount = 1;

  private volatile boolean isRunning = false;

  public SqsMessageListener(SqsClient sqsClient) {
    this.sqsClient = sqsClient;
  }

  /**
   * Set the number of consumer threads that will pole from this queue
   */
  public void setConsumerCount(int consumerCount) {
    if (isRunning) {
      adjustConsumerCount(consumerCount);
    }
    this.consumerCount = consumerCount;
  }

  /**
   * Set the thread pool that will invoke the {@linkplain MessageHandler}
   */
  public void setTaskExecutor(ExecutorService taskExecutor) {
    this.taskExecutor = taskExecutor;
  }

  /**
   * Register the SQS queue with this listener.
   *
   * @see SqsQueue
   */
  public void setQueue(SqsQueue queue) {
    this.queue = queue;
  }

  public synchronized void initialize() {
    consumerLoopExecutor = createConsumerLoopExecutor();
    taskExecutor = Objects
        .requireNonNullElseGet(taskExecutor, this::defaultMessageProcessorExecutor);
  }

  public synchronized void start() {
    if (isRunning) {
      return;
    }
    scheduleConsumers();
    isRunning = true;
    log.info("SqsMessageProcessorContainer started");
  }

  public synchronized void destroy() {
    log.info("shutting down SqsMessageProcessorContainer");
    terminate();
    log.info("SqsMessageProcessorContainer destroyed");
  }

  public boolean isRunning() {
    return isRunning;
  }

  private ExecutorService createConsumerLoopExecutor() {
    return Executors.newFixedThreadPool(consumerCount,
        ConfigurableThreadFactory.newFactory(CONSUMER_LOOP_THREAD_PREFIX));
  }

  private ExecutorService defaultMessageProcessorExecutor() {
    int poolSize = consumerCount * queue.getMaxBatchSize() + 1;
    ThreadPoolExecutor executor = new ThreadPoolExecutor(poolSize, poolSize,
        120L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(),
        ConfigurableThreadFactory.newFactory(MESSAGE_PROCESSOR_THREAD_PREFIX));

    // Allow idle core threads to time out
    executor.allowCoreThreadTimeOut(true);
    return executor;
  }

  private void scheduleConsumers() {
    for (int i = 0; i < consumerCount; i++) {
      consumerMap.computeIfAbsent(UUID.randomUUID().toString(),
          consumerId -> consumerLoopExecutor.submit(new QueueConsumer(queue, consumerId)));
    }
  }

  private void terminate() {
    consumerMap.values().forEach(consumer -> consumer.cancel(false));
    consumerMap.clear();

    taskExecutor.shutdown();
    consumerLoopExecutor.shutdown();

    try {
      boolean taskExecTerminated = taskExecutor.awaitTermination(20, TimeUnit.SECONDS);
      boolean loopExecTerminated = consumerLoopExecutor.awaitTermination(20, TimeUnit.SECONDS);
      if (!taskExecTerminated || !loopExecTerminated) {
        log.warn("SqsMessageListener did not terminate gracefully");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private boolean isActive(String consumerId) {
    Future<?> consumer = consumerMap.get(consumerId);
    return consumer != null && !consumer.isDone();
  }

  private synchronized void adjustConsumerCount(int concurrentConsumers) {

    // increase
    while (consumerMap.size() < concurrentConsumers) {
      consumerMap.computeIfAbsent(UUID.randomUUID().toString(),
          consumerId -> consumerLoopExecutor.submit(new QueueConsumer(queue, consumerId)));
    }

    // decrease
    if (consumerMap.size() > concurrentConsumers) {
      Enumeration<Future<?>> consumers = ((ConcurrentHashMap<String, Future<?>>) consumerMap)
          .elements();
      int delta = consumerMap.size() - concurrentConsumers;
      for (int i = 0; i < delta; i++) {
        if (consumers.hasMoreElements()) {
          Future<?> consumer = consumers.nextElement();
          consumer.cancel(false);
        }
      }
    }
  }

  private class QueueConsumer implements Runnable {

    private final SqsQueue queue;
    private final String id;

    private QueueConsumer(SqsQueue queue, String id) {
      this.queue = queue;
      this.id = id;
    }

    @Override
    public void run() {
      while (isActive(id)) {
        ReceiveMessageResponse response = receive(queue);

        int batchSize = response.messages().size();
        CountDownLatch completionLatch = new CountDownLatch(batchSize);

        // collect successfully processed messages to auto-acknowledge
        List<Message> successful = new ArrayList<>(batchSize);

        for (Message msg : response.messages()) {
          taskExecutor
              .submit(new MessageProcessor(queue, msg, completionLatch, successful::add));
        }

        try {
          completionLatch.await();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }

        if (queue.isAutoAcknowledge()) {
          deleteBatch(successful, queue);
        }
      }
    }
  }

  private static class MessageProcessor implements Runnable {

    private final Message message;
    private final SqsQueue queue;
    private final CountDownLatch completionLatch;
    private final Consumer<Message> onSuccess;

    private MessageProcessor(SqsQueue queue, Message message,
        CountDownLatch completionLatch,
        Consumer<Message> onSuccess) {
      this.queue = queue;
      this.message = message;
      this.completionLatch = completionLatch;
      this.onSuccess = onSuccess;
    }

    @Override
    public void run() {
      try {
        queue.getMessageHandler().handle(message);
        onSuccess.accept(message);
      } catch (Exception e) {
        ErrorHandler errorHandler = queue.getErrorHandler();
        if (errorHandler != null) {
          errorHandler.onError(message, e);
        } else {
          log.error("{} - Error processing message[{}]", queue.getUrl(), message.messageId(), e);
        }
      } finally {
        completionLatch.countDown();
      }
    }
  }

  private ReceiveMessageResponse receive(SqsQueue queue) {
    ReceiveMessageRequest request = ReceiveMessageRequest.builder()
        .queueUrl(queue.getUrl())
        .waitTimeSeconds(queue.isLongPolling() ? 20 : 1)
        .maxNumberOfMessages(queue.getMaxBatchSize())
        .visibilityTimeout(queue.getVisibilityTimeoutSeconds())
        .build();

    try {
      return sqsClient.receiveMessage(request);
    } catch (AwsServiceException | SdkClientException e) {
      log.error("SQS sdk receiveMessage error", e);

      // aws sdk exceptions usually mean that we have some network problem or actual service is down
      // pause the loop for 2 seconds
      try {
        TimeUnit.SECONDS.sleep(2);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
      }
      return ReceiveMessageResponse.builder().messages(List.of()).build();
    }
  }

  private void deleteBatch(List<Message> messages, SqsQueue queue) {
    List<DeleteMessageBatchRequestEntry> entries = messages.stream()
        .map(message -> DeleteMessageBatchRequestEntry.builder()
            .id(message.messageId())
            .receiptHandle(message.receiptHandle())
            .build())
        .collect(Collectors.toList());

    if (entries.isEmpty()) {
      return;
    }

    try {
      sqsClient.deleteMessageBatch(DeleteMessageBatchRequest.builder()
          .queueUrl(queue.getUrl())
          .entries(entries)
          .build());
    } catch (AwsServiceException | SdkClientException e) {
      log.error("SQS sdk deleteMessageBatch error", e);
    }
  }
}
