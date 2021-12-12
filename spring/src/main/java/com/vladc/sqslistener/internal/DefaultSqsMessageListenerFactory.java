package com.vladc.sqslistener.internal;

import com.vladc.sqslistener.HandlerInterceptor;
import com.vladc.sqslistener.MessageListener;
import com.vladc.sqslistener.SqsMessageListener;
import com.vladc.sqslistener.SqsMessageListenerFactory;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import software.amazon.awssdk.services.sqs.SqsClient;

public class DefaultSqsMessageListenerFactory implements SqsMessageListenerFactory {

  private final SqsClient sqsClient;
  private final ThreadPoolTaskExecutor messageProcessorTaskExecutor;
  private final List<HandlerInterceptor> filters;

  public DefaultSqsMessageListenerFactory(
      SqsClient sqsClient,
      ThreadPoolTaskExecutor messageProcessorTaskExecutor,
      List<HandlerInterceptor> filters) {
    this.sqsClient = sqsClient;
    this.messageProcessorTaskExecutor = messageProcessorTaskExecutor;
    this.filters = filters;
  }

  @Override
  public MessageListener createListener(SqsQueueAttributes attributes) {
    return SqsMessageListener.builder()
        .client(sqsClient)
        .executor(getMessageProcessorExecutor())
        .url(attributes.getUrl())
        .handler(attributes.getHandler())
        .errorHandler(attributes.getErrorHandler())
        .concurrency(attributes.getConcurrency())
        .maxBatchSize(attributes.getMaxBatchSize())
        .visibilityTimeoutSeconds(attributes.getVisibilityTimeoutSeconds())
        .longPolling(attributes.getLongPolling())
        .autoAcknowledge(attributes.getAutoAcknowledge())
        .interceptors(filters == null ? List.of() : filters)
        .build();
  }

  private ExecutorService getMessageProcessorExecutor() {
    return messageProcessorTaskExecutor == null
        ? null
        : messageProcessorTaskExecutor.getThreadPoolExecutor();
  }
}
