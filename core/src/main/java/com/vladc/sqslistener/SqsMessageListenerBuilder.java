package com.vladc.sqslistener;

import java.util.concurrent.ExecutorService;
import software.amazon.awssdk.services.sqs.SqsClient;

public class SqsMessageListenerBuilder {

  private SqsClient sqsClient;
  private SqsQueue queue;
  private ExecutorService executor;
  private int consumerCount;
  private boolean autoStart;

  public SqsMessageListenerBuilder client(SqsClient client) {
    this.sqsClient = client;
    return this;
  }

  public SqsMessageListenerBuilder queue(SqsQueue queue) {
    this.queue = queue;
    return this;
  }

  public SqsMessageListenerBuilder consumerCount(int consumerCount) {
    this.consumerCount = consumerCount;
    return this;
  }

  public SqsMessageListenerBuilder executor(ExecutorService executor) {
    this.executor = executor;
    return this;
  }

  public SqsMessageListenerBuilder autoStart(boolean autoStart) {
    this.autoStart = autoStart;
    return this;
  }

  public SqsMessageListener build() {
    return new SqsMessageListener(this.sqsClient, this.consumerCount, this.executor, this.queue,
        this.autoStart);
  }
}
