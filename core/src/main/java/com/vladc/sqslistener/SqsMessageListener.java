package com.vladc.sqslistener;

import static java.util.Objects.requireNonNullElseGet;

import java.util.ArrayList;
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
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.AbortedException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

@Slf4j
public class SqsMessageListener implements MessageListener {

  private static final String CONSUMER_LOOP_THREAD_PREFIX = "sqs-consumer-loop";
  private static final String MESSAGE_PROCESSOR_THREAD_PREFIX = "sqs-listener";

  private final SqsQueue queue;
  private final SqsClient sqsClient;

  private final Map<String, Future<?>> consumerMap = new ConcurrentHashMap<>();
  private final Object monitor = new Object();
  private volatile boolean isRunning = false;

  private ExecutorService taskExecutor;
  private ExecutorService consumerLoopExecutor;

  public static SqsMessageListenerBuilder builder() {
    return new SqsMessageListenerBuilder();
  }

  SqsMessageListener(SqsClient sqsClient, ExecutorService taskExecutor, SqsQueue queue) {
    this.sqsClient = Objects.requireNonNull(sqsClient);
    this.queue = Objects.requireNonNull(queue);
    this.taskExecutor = taskExecutor;
  }

  @Override
  public void subscribe() {
    synchronized (monitor) {
      if (isRunning) {
        return;
      }
      log.info("{} - starting SqsMessageListener", queue.getUrl());
      consumerLoopExecutor = createConsumerLoopExecutor();
      taskExecutor = requireNonNullElseGet(taskExecutor, this::defaultMessageProcessorExecutor);
      scheduleConsumers();
      isRunning = true;
      log.info("{} - SqsMessageListener started", queue.getUrl());
    }
  }

  @Override
  public void destroy() {
    synchronized (monitor) {
      log.info("{} - shutting down SqsMessageListener", queue.getUrl());
      isRunning = false;
      consumerMap.values().forEach(consumer -> consumer.cancel(false));
      consumerMap.clear();

      taskExecutor.shutdown();
      consumerLoopExecutor.shutdown();
    }
  }

  @Override
  public void awaitTermination(long timeout, TimeUnit timeUnit) throws InterruptedException {
    boolean terminated = consumerLoopExecutor.awaitTermination(timeout, timeUnit);
    if (!terminated) {
      consumerLoopExecutor.shutdownNow();
    }
  }

  SqsQueue getQueue() {
    return queue;
  }

  private ExecutorService createConsumerLoopExecutor() {
    return Executors.newFixedThreadPool(
        queue.getConcurrency(), new PrefixedThreadFactory(CONSUMER_LOOP_THREAD_PREFIX));
  }

  private ExecutorService defaultMessageProcessorExecutor() {
    int poolSize = queue.getConcurrency() * queue.getMaxBatchSize() + 1;
    ThreadPoolExecutor executor =
        new ThreadPoolExecutor(
            poolSize,
            poolSize,
            120L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            new PrefixedThreadFactory(MESSAGE_PROCESSOR_THREAD_PREFIX));

    // Allow idle core threads to time out
    executor.allowCoreThreadTimeOut(true);
    return executor;
  }

  private void scheduleConsumers() {
    for (int i = 0; i < queue.getConcurrency(); i++) {
      consumerMap.computeIfAbsent(
          UUID.randomUUID().toString(),
          consumerId -> consumerLoopExecutor.submit(new QueueConsumer(consumerId)));
    }
  }

  private boolean isActive(String consumerId) {
    Future<?> consumer = consumerMap.get(consumerId);
    return consumer != null && !consumer.isDone();
  }

  private ReceiveMessageResponse receive() {
    ReceiveMessageRequest request =
        ReceiveMessageRequest.builder()
            .queueUrl(queue.getUrl())
            .waitTimeSeconds(queue.getLongPolling() ? 20 : 1)
            .maxNumberOfMessages(queue.getMaxBatchSize())
            .visibilityTimeout(queue.getVisibilityTimeoutSeconds())
            .messageAttributeNames("All")
            .build();

    try {
      return sqsClient.receiveMessage(request);
    } catch (AbortedException ae) {
      log.error("{} - receive interrupted", queue.getUrl());
      Thread.currentThread().interrupt();
      return ReceiveMessageResponse.builder().messages(List.of()).build();
    } catch (SdkClientException | SqsException e) {
      log.error("{} - SQS sdk receiveMessage error", queue.getUrl(), e);

      // aws sdk exceptions usually mean that we have some network problem or the service is down
      // pause the loop for 5 seconds
      try {
        TimeUnit.SECONDS.sleep(5);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
      }
      return ReceiveMessageResponse.builder().messages(List.of()).build();
    } catch (Exception e) {
      return ReceiveMessageResponse.builder().messages(List.of()).build();
    }
  }

  private void deleteBatch(List<Message> messages) {
    List<DeleteMessageBatchRequestEntry> entries =
        messages.stream()
            .map(
                message ->
                    DeleteMessageBatchRequestEntry.builder()
                        .id(message.messageId())
                        .receiptHandle(message.receiptHandle())
                        .build())
            .collect(Collectors.toList());

    if (entries.isEmpty()) {
      return;
    }

    try {
      sqsClient.deleteMessageBatch(
          DeleteMessageBatchRequest.builder().queueUrl(queue.getUrl()).entries(entries).build());
    } catch (AwsServiceException | SdkClientException e) {
      log.error("{} - SQS sdk deleteMessageBatch error", queue.getUrl(), e);
    }
  }

  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  private class QueueConsumer implements Runnable {

    private final String id;

    @Override
    public void run() {
      while (isActive(id)) {
        try {
          ReceiveMessageResponse response = receive();

          int batchSize = response.messages().size();
          CountDownLatch completionLatch = new CountDownLatch(batchSize);
          List<Message> successful = new ArrayList<>(batchSize);

          for (Message msg : response.messages()) {
            taskExecutor.submit(new MessageProcessor(msg, queue, completionLatch, successful::add));
          }

          try {
            completionLatch.await();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }

          if (queue.getAutoAcknowledge()) {
            deleteBatch(successful);
          }
        } catch (Throwable t) {
          log.error("{} - Unhandled exception in QueueConsumer", queue.getUrl(), t);
        }
      }
    }
  }

  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  private static class MessageProcessor implements Runnable {

    private final Message message;
    private final SqsQueue queue;
    private final CountDownLatch completionLatch;
    private final Consumer<Message> onSuccess;

    @Override
    public void run() {
      try {
        queue.getInterceptors().forEach(f -> f.beforeHandle(message));
        queue.getHandler().handle(message);
        onSuccess.accept(message);
      } catch (Exception e) {
        ErrorHandler errorHandler = queue.getErrorHandler();
        if (errorHandler != null) {
          errorHandler.onError(message, e);
        } else {
          log.error("{} failed to process message {}", queue.getUrl(), message.messageId(), e);
        }
      } finally {
        MDC.clear();
        completionLatch.countDown();
      }
    }
  }
}
